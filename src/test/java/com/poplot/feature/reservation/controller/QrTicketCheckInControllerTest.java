package com.poplot.feature.reservation.controller;

import com.jayway.jsonpath.JsonPath;
import com.poplot.feature.event.entity.Event;
import com.poplot.feature.event.enums.EventStatus;
import com.poplot.feature.reservation.entity.QrTicket;
import com.poplot.feature.reservation.entity.Reservation;
import com.poplot.feature.reservation.entity.TimeSlot;
import com.poplot.feature.reservation.enums.ReservationStatus;
import com.poplot.feature.reservation.repository.QrTicketRepository;
import com.poplot.feature.reservation.repository.ReservationRepository;
import com.poplot.feature.reservation.token.QrTokenProvider;
import com.poplot.feature.store.entity.Store;
import com.poplot.feature.user.entity.User;
import com.poplot.feature.user.enums.UserRole;
import com.poplot.feature.user.enums.UserStatus;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
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
    "DELETE FROM waitlist",
    "DELETE FROM time_slot",
    "DELETE FROM event",
    "DELETE FROM store",
    "DELETE FROM users"
})
class QrTicketCheckInControllerTest {
    private static final ZoneId RESERVATION_ZONE = ZoneId.of("Asia/Seoul");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ReservationRepository reservationRepository;
    @Autowired
    private QrTicketRepository qrTicketRepository;
    @Autowired
    private QrTokenProvider qrTokenProvider;
    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("owner repeatedly retrieves one stable QR ticket while only its hash is stored")
    void ownerRetrievesStableHashedTicket() throws Exception {
        User owner = saveUser("owner@example.com", UserRole.USER);
        Reservation reservation = persistReservation(owner, ReservationStatus.CONFIRMED, currentSlotStart(), 30, 30);

        String firstToken = getTicketToken(owner, reservation);
        String secondToken = getTicketToken(owner, reservation);

        assertThat(secondToken).isEqualTo(firstToken);
        assertThat(qrTicketRepository.count()).isEqualTo(1);
        QrTicket ticket = qrTicketRepository.findByReservationId(reservation.getId()).orElseThrow();
        assertThat(ticket.getTokenHash()).isEqualTo(qrTokenProvider.hash(firstToken));
        assertThat(ticket.getTokenHash()).doesNotContain(firstToken);
    }

    @Test
    @DisplayName("non-owner and non-confirmed reservation cannot retrieve a QR ticket")
    void rejectsUnauthorizedOrUnavailableTicket() throws Exception {
        User owner = saveUser("owner@example.com", UserRole.USER);
        User other = saveUser("other@example.com", UserRole.USER);
        Reservation confirmed = persistReservation(owner, ReservationStatus.CONFIRMED, currentSlotStart(), 30, 30);

        mockMvc.perform(get("/reservations/{publicId}/ticket", confirmed.getPublicId())
                .header("Authorization", bearerToken(other)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("RESERVATION_403_01"));

        Reservation pending = persistReservation(owner, ReservationStatus.PENDING, currentSlotStart().plusHours(1), 30, 30);
        mockMvc.perform(get("/reservations/{publicId}/ticket", pending.getPublicId())
                .header("Authorization", bearerToken(owner)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("RESERVATION_409_08"));
    }

    @Test
    @DisplayName("staff checks in a confirmed reservation once within the allowed time")
    void staffChecksInConfirmedReservationOnce() throws Exception {
        User owner = saveUser("owner@example.com", UserRole.USER);
        User staff = saveUser("staff@example.com", UserRole.STAFF);
        Reservation reservation = persistReservation(owner, ReservationStatus.CONFIRMED, currentSlotStart(), 30, 30);
        String qrToken = getTicketToken(owner, reservation);

        mockMvc.perform(post("/staff/check-ins")
                .header("Authorization", bearerToken(staff))
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkInBody(qrToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.reservationPublicId").value(reservation.getPublicId().toString()))
            .andExpect(jsonPath("$.data.checkInStatus").value("CHECKED_IN"))
            .andExpect(jsonPath("$.data.checkedInAt").isNotEmpty())
            .andExpect(jsonPath("$.data.partySize").value(2));

        mockMvc.perform(post("/staff/check-ins")
                .header("Authorization", bearerToken(staff))
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkInBody(qrToken)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CHECKIN_409_01"));

        Reservation saved = reservationRepository.findById(reservation.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ReservationStatus.CHECKED_IN);
        assertThat(saved.getCheckedInAt()).isNotNull();
    }

    @Test
    @DisplayName("secret rotation invalidates an old ticket until the owner reissues it")
    void secretRotationInvalidatesOldTicketUntilReissue() throws Exception {
        User owner = saveUser("rotation-owner@example.com", UserRole.USER);
        User staff = saveUser("rotation-staff@example.com", UserRole.STAFF);
        Reservation reservation = persistReservation(owner, ReservationStatus.CONFIRMED, currentSlotStart(), 30, 30);
        QrTokenProvider previousProvider = new QrTokenProvider("previous-qr-ticket-secret-key-123456789");
        String previousToken = previousProvider.createToken(reservation.getPublicId());
        transactionTemplate.executeWithoutResult(status -> entityManager.persist(new QrTicket(
            reservationRepository.findById(reservation.getId()).orElseThrow(),
            previousProvider.hash(previousToken),
            previousProvider.getKeyId(),
            Instant.now()
        )));

        mockMvc.perform(post("/staff/check-ins")
                .header("Authorization", bearerToken(staff))
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkInBody(previousToken)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("CHECKIN_404_01"));

        String currentToken = getTicketToken(owner, reservation);
        QrTicket reissued = qrTicketRepository.findByReservationId(reservation.getId()).orElseThrow();
        assertThat(reissued.getKeyId()).isEqualTo(qrTokenProvider.getKeyId());
        assertThat(reissued.getTokenHash()).isEqualTo(qrTokenProvider.hash(currentToken));

        mockMvc.perform(post("/staff/check-ins")
                .header("Authorization", bearerToken(staff))
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkInBody(currentToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.checkInStatus").value("CHECKED_IN"));
    }

    @Test
    @DisplayName("check-in validates role, token presence, and token authenticity")
    void validatesStaffAndQrToken() throws Exception {
        User user = saveUser("user@example.com", UserRole.USER);
        User staff = saveUser("staff@example.com", UserRole.STAFF);

        mockMvc.perform(post("/staff/check-ins")
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkInBody("forged-token")))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("CHECKIN_403_01"));
        mockMvc.perform(post("/staff/check-ins")
                .header("Authorization", bearerToken(staff))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"qrToken\":\" \"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("CHECKIN_400_01"));
        mockMvc.perform(post("/staff/check-ins")
                .header("Authorization", bearerToken(staff))
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkInBody("forged-token")))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("CHECKIN_404_01"));
    }

    @Test
    @DisplayName("staff cannot check in invalid reservation states or outside the configured time window")
    void rejectsInvalidStatusAndTimeWindow() throws Exception {
        User owner = saveUser("owner@example.com", UserRole.USER);
        User staff = saveUser("staff@example.com", UserRole.STAFF);
        Reservation pending = persistReservation(owner, ReservationStatus.PENDING, currentSlotStart(), 30, 30);
        String pendingToken = persistTicketToken(pending);

        mockMvc.perform(post("/staff/check-ins")
                .header("Authorization", bearerToken(staff))
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkInBody(pendingToken)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CHECKIN_409_02"));

        Reservation future = persistReservation(owner, ReservationStatus.CONFIRMED, currentSlotStart().plusDays(1), 30, 30);
        String futureToken = getTicketToken(owner, future);
        mockMvc.perform(post("/staff/check-ins")
                .header("Authorization", bearerToken(staff))
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkInBody(futureToken)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("CHECKIN_409_03"));
    }

    private String getTicketToken(User owner, Reservation reservation) throws Exception {
        MvcResult result = mockMvc.perform(get("/reservations/{publicId}/ticket", reservation.getPublicId())
                .header("Authorization", bearerToken(owner)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.eventName").value("event"))
            .andExpect(jsonPath("$.data.partySize").value(2))
            .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.qrToken");
    }

    private String persistTicketToken(Reservation reservation) {
        String token = qrTokenProvider.createToken(reservation.getPublicId());
        transactionTemplate.executeWithoutResult(status -> entityManager.persist(
            new QrTicket(
                reservationRepository.findById(reservation.getId()).orElseThrow(),
                qrTokenProvider.hash(token),
                qrTokenProvider.getKeyId(),
                Instant.now()
            )
        ));
        return token;
    }

    private Reservation persistReservation(
        User owner,
        ReservationStatus status,
        LocalDateTime slotStart,
        Integer checkInOpenMinutes,
        Integer checkInCloseMinutes
    ) {
        return transactionTemplate.execute(transactionStatus -> {
            Store store = new Store("store-" + UUID.randomUUID());
            entityManager.persist(store);
            Event event = new Event(
                store,
                "event",
                Instant.now().minusSeconds(3_600),
                Instant.now().plusSeconds(3_600),
                slotStart.toLocalDate(),
                slotStart.toLocalDate(),
                4,
                10
            );
            ReflectionTestUtils.setField(event, "status", EventStatus.RESERVATION_OPEN);
            ReflectionTestUtils.setField(event, "checkInOpenMinutes", checkInOpenMinutes);
            ReflectionTestUtils.setField(event, "checkInCloseMinutes", checkInCloseMinutes);
            entityManager.persist(event);
            TimeSlot timeSlot = new TimeSlot(
                event,
                slotStart.toLocalDate(),
                slotStart.toLocalTime(),
                slotStart.plusHours(1).toLocalTime(),
                10
            );
            timeSlot.reserve(2);
            entityManager.persist(timeSlot);
            Reservation reservation = new Reservation(owner, timeSlot, "R-" + UUID.randomUUID(), 2);
            ReflectionTestUtils.setField(reservation, "status", status);
            entityManager.persist(reservation);
            entityManager.flush();
            return reservation;
        });
    }

    private User saveUser(String email, UserRole role) {
        return userRepository.save(User.builder()
            .email(email)
            .name(role.name().toLowerCase())
            .role(role)
            .status(UserStatus.ACTIVE)
            .build());
    }

    private LocalDateTime currentSlotStart() {
        return LocalDateTime.now(RESERVATION_ZONE).truncatedTo(ChronoUnit.MINUTES);
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtProvider.createAccessToken(user.getId());
    }

    private String checkInBody(String qrToken) {
        return "{\"qrToken\":\"" + qrToken + "\"}";
    }
}
