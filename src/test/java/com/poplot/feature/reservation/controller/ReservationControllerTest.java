package com.poplot.feature.reservation.controller;

import com.poplot.feature.event.entity.Event;
import com.poplot.feature.event.enums.EventStatus;
import com.poplot.feature.reservation.entity.TimeSlot;
import com.poplot.feature.reservation.enums.TimeSlotStatus;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Sql(statements = {
    "DELETE FROM qr_ticket",
    "DELETE FROM reservation",
    "DELETE FROM waitlist",
    "DELETE FROM time_slot",
    "DELETE FROM event",
    "DELETE FROM store",
    "DELETE FROM users"
})
class ReservationControllerTest {
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
    @DisplayName("authenticated user creates a pending reservation and consumes capacity")
    void createsPendingReservationAndConsumesCapacity() throws Exception {
        User user = userRepository.save(User.create("user@example.com", "user"));
        TimeSlot timeSlot = persistTimeSlot(EventStatus.RESERVATION_OPEN, TimeSlotStatus.OPEN, 10, 0);
        long initialVersion = timeSlot.getVersion();
        Instant beforeRequest = Instant.now();

        mockMvc.perform(post("/reservations")
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody(timeSlot.getPublicId(), 2)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.status").value("PENDING"))
            .andExpect(jsonPath("$.data.partySize").value(2))
            .andExpect(jsonPath("$.data.event.name").value("event"))
            .andExpect(jsonPath("$.data.timeSlot.timeSlotPublicId").value(timeSlot.getPublicId().toString()));

        TimeSlot savedTimeSlot = timeSlotRepository.findById(timeSlot.getId()).orElseThrow();
        assertThat(savedTimeSlot.getReservedCount()).isEqualTo(2);
        assertThat(savedTimeSlot.getVersion()).isEqualTo(initialVersion + 1);
        transactionTemplate.executeWithoutResult(status ->
            assertThat(reservationRepository.findAll()).singleElement().satisfies(reservation -> {
                assertThat(reservation.getUser().getId()).isEqualTo(user.getId());
                assertThat(reservation.getHoldExpiresAt()).isBetween(
                    beforeRequest.plus(10, ChronoUnit.MINUTES),
                    Instant.now().plus(10, ChronoUnit.MINUTES)
                );
            })
        );
    }

    @Test
    @DisplayName("reservation creation requires authentication")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post("/reservations").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_401_04"));
    }

    @Test
    @DisplayName("invalid party size and missing time slot return contract errors")
    void validatesPartySizeAndTimeSlot() throws Exception {
        User user = userRepository.save(User.create("user@example.com", "user"));

        mockMvc.perform(post("/reservations")
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"timeSlotPublicId\":\"" + UUID.randomUUID() + "\",\"partySize\":0}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("RESERVATION_400_01"));

        mockMvc.perform(post("/reservations")
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"timeSlotPublicId\":\"" + UUID.randomUUID() + "\",\"partySize\":1}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("TIMESLOT_404_01"));
    }

    @Test
    @DisplayName("closed event or time slot cannot be reserved")
    void rejectsUnavailableTimeSlot() throws Exception {
        User user = userRepository.save(User.create("user@example.com", "user"));
        TimeSlot timeSlot = persistTimeSlot(EventStatus.SCHEDULED, TimeSlotStatus.OPEN, 10, 0);

        mockMvc.perform(post("/reservations")
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody(timeSlot.getPublicId(), 1)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("RESERVATION_400_02"));

        TimeSlot savedTimeSlot = timeSlotRepository.findById(timeSlot.getId()).orElseThrow();
        assertThat(savedTimeSlot.getReservedCount()).isZero();
        assertThat(reservationRepository.count()).isZero();
    }

    @Test
    @DisplayName("party size cannot exceed the remaining capacity")
    void rejectsInsufficientCapacityWithoutPartialChanges() throws Exception {
        User user = userRepository.save(User.create("user@example.com", "user"));
        TimeSlot timeSlot = persistTimeSlot(EventStatus.RESERVATION_OPEN, TimeSlotStatus.OPEN, 10, 8);

        mockMvc.perform(post("/reservations")
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody(timeSlot.getPublicId(), 3)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("RESERVATION_409_01"));

        TimeSlot savedTimeSlot = timeSlotRepository.findById(timeSlot.getId()).orElseThrow();
        assertThat(savedTimeSlot.getReservedCount()).isEqualTo(8);
        assertThat(reservationRepository.count()).isZero();
    }

    @Test
    @DisplayName("user cannot create another active reservation for the same time slot")
    void rejectsDuplicateActiveReservation() throws Exception {
        User user = userRepository.save(User.create("user@example.com", "user"));
        TimeSlot timeSlot = persistTimeSlot(EventStatus.RESERVATION_OPEN, TimeSlotStatus.OPEN, 10, 0);
        String request = requestBody(timeSlot.getPublicId(), 1);

        mockMvc.perform(post("/reservations")
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isCreated());
        mockMvc.perform(post("/reservations")
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(request))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("RESERVATION_409_02"));

        TimeSlot savedTimeSlot = timeSlotRepository.findById(timeSlot.getId()).orElseThrow();
        assertThat(reservationRepository.count()).isEqualTo(1);
        assertThat(savedTimeSlot.getReservedCount()).isEqualTo(1);
    }

    private TimeSlot persistTimeSlot(
        EventStatus eventStatus,
        TimeSlotStatus timeSlotStatus,
        int capacity,
        int reservedCount
    ) {
        return transactionTemplate.execute(status -> {
            Store store = new Store("store");
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
            ReflectionTestUtils.setField(event, "status", eventStatus);
            entityManager.persist(event);
            TimeSlot timeSlot = new TimeSlot(
                event,
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                capacity
            );
            ReflectionTestUtils.setField(timeSlot, "status", timeSlotStatus);
            ReflectionTestUtils.setField(timeSlot, "reservedCount", reservedCount);
            entityManager.persist(timeSlot);
            entityManager.flush();
            return timeSlot;
        });
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtProvider.createAccessToken(user.getId());
    }

    private String requestBody(UUID timeSlotPublicId, int partySize) {
        return "{\"timeSlotPublicId\":\"" + timeSlotPublicId + "\",\"partySize\":" + partySize + "}";
    }
}
