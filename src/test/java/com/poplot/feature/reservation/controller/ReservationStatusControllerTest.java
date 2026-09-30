package com.poplot.feature.reservation.controller;

import com.poplot.feature.event.entity.Event;
import com.poplot.feature.event.enums.EventStatus;
import com.poplot.feature.reservation.entity.Reservation;
import com.poplot.feature.reservation.entity.TimeSlot;
import com.poplot.feature.reservation.enums.ReservationStatus;
import com.poplot.feature.reservation.repository.ReservationRepository;
import com.poplot.feature.reservation.repository.TimeSlotRepository;
import com.poplot.feature.store.entity.Store;
import com.poplot.feature.user.entity.User;
import com.poplot.feature.user.repository.UserRepository;
import com.poplot.global.security.jwt.JwtProvider;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Sql(statements = {
    "DELETE FROM qr_ticket",
    "DELETE FROM reservation",
    "DELETE FROM time_slot",
    "DELETE FROM event",
    "DELETE FROM store",
    "DELETE FROM users"
})
class ReservationStatusControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private TimeSlotRepository timeSlotRepository;
    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("owner retrieves reservation details")
    void retrievesReservationDetails() throws Exception {
        User owner = userRepository.save(User.create("owner@example.com", "owner"));
        Reservation reservation = persistReservation(owner, ReservationStatus.PENDING, Instant.now().plusSeconds(600), null);

        mockMvc.perform(get("/reservations/{publicId}", reservation.getPublicId())
                .header("Authorization", bearerToken(owner)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.reservationPublicId").value(reservation.getPublicId().toString()))
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andExpect(jsonPath("$.data.partySize").value(2))
            .andExpect(jsonPath("$.data.event.name").value("event"))
            .andExpect(jsonPath("$.data.timeSlot.startTime").value("10:00"));
    }

    @Test
    @DisplayName("non-owner cannot retrieve, confirm, or cancel reservation")
    void rejectsNonOwner() throws Exception {
        User owner = userRepository.save(User.create("owner@example.com", "owner"));
        User other = userRepository.save(User.create("other@example.com", "other"));
        Reservation reservation = persistReservation(owner, ReservationStatus.PENDING, Instant.now().plusSeconds(600), null);
        Long timeSlotId = reservation.getTimeSlot().getId();

        mockMvc.perform(get("/reservations/{publicId}", reservation.getPublicId())
                .header("Authorization", bearerToken(other)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("RESERVATION_403_01"));
        mockMvc.perform(post("/reservations/{publicId}/confirm", reservation.getPublicId())
                .header("Authorization", bearerToken(other)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("RESERVATION_403_01"));
        mockMvc.perform(post("/reservations/{publicId}/cancel", reservation.getPublicId())
                .header("Authorization", bearerToken(other)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("RESERVATION_403_01"));

        assertThat(reservationRepository.findById(reservation.getId()).orElseThrow().getStatus())
            .isEqualTo(ReservationStatus.PENDING);
        assertThat(timeSlotRepository.findById(timeSlotId).orElseThrow().getReservedCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("missing or malformed reservation public id returns not found")
    void returnsNotFound() throws Exception {
        User owner = userRepository.save(User.create("owner@example.com", "owner"));

        mockMvc.perform(get("/reservations/not-a-uuid").header("Authorization", bearerToken(owner)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RESERVATION_404_01"));
        mockMvc.perform(get("/reservations/{publicId}", UUID.randomUUID()).header("Authorization", bearerToken(owner)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RESERVATION_404_01"));
    }

    @Test
    @DisplayName("pending reservation is confirmed once before expiration")
    void confirmsPendingReservation() throws Exception {
        User owner = userRepository.save(User.create("owner@example.com", "owner"));
        Reservation reservation = persistReservation(owner, ReservationStatus.PENDING, Instant.now().plusSeconds(600), null);

        mockMvc.perform(post("/reservations/{publicId}/confirm", reservation.getPublicId())
                .header("Authorization", bearerToken(owner)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.data.confirmedAt").isNotEmpty())
            .andExpect(jsonPath("$.data.canceledAt").doesNotExist());
        mockMvc.perform(post("/reservations/{publicId}/confirm", reservation.getPublicId())
                .header("Authorization", bearerToken(owner)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("RESERVATION_409_04"));
    }

    @Test
    @DisplayName("expired pending reservation cannot be confirmed")
    void rejectsExpiredConfirmation() throws Exception {
        User owner = userRepository.save(User.create("owner@example.com", "owner"));
        Reservation reservation = persistReservation(owner, ReservationStatus.PENDING, Instant.now().minusSeconds(1), null);

        mockMvc.perform(post("/reservations/{publicId}/confirm", reservation.getPublicId())
                .header("Authorization", bearerToken(owner)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("RESERVATION_409_03"));

        assertThat(reservationRepository.findById(reservation.getId()).orElseThrow().getStatus())
            .isEqualTo(ReservationStatus.PENDING);
    }

    @Test
    @DisplayName("canceling reservation restores capacity and cannot be repeated")
    void cancelsAndRestoresCapacity() throws Exception {
        User owner = userRepository.save(User.create("owner@example.com", "owner"));
        Reservation reservation = persistReservation(owner, ReservationStatus.CONFIRMED, Instant.now().plusSeconds(600), 60);
        Long timeSlotId = reservation.getTimeSlot().getId();

        mockMvc.perform(post("/reservations/{publicId}/cancel", reservation.getPublicId())
                .header("Authorization", bearerToken(owner)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("CANCELED"))
            .andExpect(jsonPath("$.data.canceledAt").isNotEmpty())
            .andExpect(jsonPath("$.data.confirmedAt").doesNotExist());

        assertThat(timeSlotRepository.findById(timeSlotId).orElseThrow().getReservedCount()).isZero();
        mockMvc.perform(post("/reservations/{publicId}/cancel", reservation.getPublicId())
                .header("Authorization", bearerToken(owner)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("RESERVATION_409_05"));
        assertThat(timeSlotRepository.findById(timeSlotId).orElseThrow().getReservedCount()).isZero();
    }

    @Test
    @DisplayName("reservation cannot be canceled after event cancellation deadline")
    void rejectsLateCancellation() throws Exception {
        User owner = userRepository.save(User.create("owner@example.com", "owner"));
        Reservation reservation = persistReservation(owner, ReservationStatus.CONFIRMED, Instant.now().plusSeconds(600), 3_000);
        Long timeSlotId = reservation.getTimeSlot().getId();

        mockMvc.perform(post("/reservations/{publicId}/cancel", reservation.getPublicId())
                .header("Authorization", bearerToken(owner)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("RESERVATION_409_06"));

        assertThat(reservationRepository.findById(reservation.getId()).orElseThrow().getStatus())
            .isEqualTo(ReservationStatus.CONFIRMED);
        assertThat(timeSlotRepository.findById(timeSlotId).orElseThrow().getReservedCount()).isEqualTo(2);
    }

    private Reservation persistReservation(
        User owner,
        ReservationStatus status,
        Instant holdExpiresAt,
        Integer cancellationDeadlineMinutes
    ) {
        return transactionTemplate.execute(transactionStatus -> {
            Store store = new Store("store");
            ReflectionTestUtils.setField(store, "bannerImgUrl", "https://example.com/banner.jpg");
            entityManager.persist(store);
            Event event = new Event(
                store,
                "event",
                Instant.now().minusSeconds(3_600),
                Instant.now().plusSeconds(3_600),
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(2),
                4,
                10
            );
            ReflectionTestUtils.setField(event, "status", EventStatus.RESERVATION_OPEN);
            ReflectionTestUtils.setField(event, "cancellationDeadlineMinutes", cancellationDeadlineMinutes);
            entityManager.persist(event);
            TimeSlot timeSlot = new TimeSlot(
                event,
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                10
            );
            timeSlot.reserve(2);
            entityManager.persist(timeSlot);
            Reservation reservation = new Reservation(owner, timeSlot, "R-" + UUID.randomUUID(), 2, holdExpiresAt);
            ReflectionTestUtils.setField(reservation, "status", status);
            entityManager.persist(reservation);
            entityManager.flush();
            return reservation;
        });
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtProvider.createAccessToken(user.getId());
    }
}
