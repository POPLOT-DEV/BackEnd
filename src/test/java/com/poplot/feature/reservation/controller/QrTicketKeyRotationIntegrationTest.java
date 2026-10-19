package com.poplot.feature.reservation.controller;

import com.jayway.jsonpath.JsonPath;
import com.poplot.feature.event.entity.Event;
import com.poplot.feature.event.enums.EventStatus;
import com.poplot.feature.reservation.entity.QrTicket;
import com.poplot.feature.reservation.entity.Reservation;
import com.poplot.feature.reservation.entity.TimeSlot;
import com.poplot.feature.reservation.enums.QrTicketKeyStatus;
import com.poplot.feature.reservation.enums.QrTicketRevocationReason;
import com.poplot.feature.reservation.enums.ReservationStatus;
import com.poplot.feature.reservation.repository.QrTicketKeyRepository;
import com.poplot.feature.reservation.repository.QrTicketRepository;
import com.poplot.feature.reservation.repository.ReservationRepository;
import com.poplot.feature.reservation.service.QrTicketKeyService;
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
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest(properties = {"spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=validate"})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@Sql(statements = {
    "DELETE FROM qr_ticket",
    "DELETE FROM qr_ticket_key",
    "DELETE FROM reservation",
    "DELETE FROM waitlist",
    "DELETE FROM time_slot",
    "DELETE FROM event",
    "DELETE FROM store",
    "DELETE FROM users"
})
class QrTicketKeyRotationIntegrationTest {
    private static final ZoneId RESERVATION_ZONE = ZoneId.of("Asia/Seoul");

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

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
    private QrTicketKeyRepository qrTicketKeyRepository;
    @Autowired
    private QrTicketKeyService qrTicketKeyService;
    @Autowired
    private QrTokenProvider qrTokenProvider;
    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("instance still configured with a rotated-out key rejects its tickets and cannot reactivate the key")
    void instanceWithRetiredKeyRejectsTicketsAndCannotReactivate() throws Exception {
        User owner = saveUser("owner@example.com", UserRole.USER);
        User staff = saveUser("staff@example.com", UserRole.STAFF);
        Reservation reservation = persistConfirmedReservation(owner);
        qrTicketKeyService.activateCurrentKey();
        String retiredKeyToken = getTicketToken(owner, reservation);

        QrTokenProvider nextProvider = new QrTokenProvider("next-qr-ticket-secret-key-for-poplot-123456789");
        int revokedCount = activateWith(nextProvider);

        assertThat(revokedCount).isEqualTo(1);
        QrTicket revoked = qrTicketRepository.findByReservationId(reservation.getId()).orElseThrow();
        assertThat(revoked.getRevokedAt()).isNotNull();
        assertThat(revoked.getRevokedReason()).isEqualTo(QrTicketRevocationReason.KEY_ROTATED);
        assertThat(qrTicketKeyRepository.findById(qrTokenProvider.getKeyId()).orElseThrow().getStatus())
            .isEqualTo(QrTicketKeyStatus.RETIRED);

        mockMvc.perform(post("/staff/check-ins")
                .header("Authorization", bearerToken(staff))
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkInBody(retiredKeyToken)))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value("CHECKIN_503_01"));
        mockMvc.perform(get("/reservations/{publicId}/ticket", reservation.getPublicId())
                .header("Authorization", bearerToken(owner)))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value("RESERVATION_503_01"));
        mockMvc.perform(get("/actuator/health/readiness"))
            .andExpect(status().isServiceUnavailable());

        assertThatThrownBy(qrTicketKeyService::activateCurrentKey).isInstanceOf(IllegalStateException.class);
        assertThat(qrTicketKeyRepository.findById(qrTokenProvider.getKeyId()).orElseThrow().getStatus())
            .isEqualTo(QrTicketKeyStatus.RETIRED);
        assertThat(qrTicketKeyRepository.findById(nextProvider.getKeyId()).orElseThrow().getStatus())
            .isEqualTo(QrTicketKeyStatus.ACTIVE);
        assertThat(qrTicketRepository.findByReservationId(reservation.getId()).orElseThrow().getRevokedAt())
            .isNotNull();
    }

    @Test
    @DisplayName("key rotation revokes old tickets until the owner reissues them with the active key")
    void rotationRevokesOldTicketUntilReissue() throws Exception {
        User owner = saveUser("owner@example.com", UserRole.USER);
        User staff = saveUser("staff@example.com", UserRole.STAFF);
        Reservation reservation = persistConfirmedReservation(owner);
        QrTokenProvider previousProvider = new QrTokenProvider("previous-qr-ticket-secret-key-123456789");
        activateWith(previousProvider);
        String previousToken = previousProvider.createToken(reservation.getPublicId());
        transactionTemplate.executeWithoutResult(status -> entityManager.persist(new QrTicket(
            reservationRepository.findById(reservation.getId()).orElseThrow(),
            previousProvider.hash(previousToken),
            previousProvider.getKeyId(),
            Instant.now()
        )));

        assertThat(qrTicketKeyService.activateCurrentKey()).isEqualTo(1);
        mockMvc.perform(post("/staff/check-ins")
                .header("Authorization", bearerToken(staff))
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkInBody(previousToken)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("CHECKIN_404_01"));

        String currentToken = getTicketToken(owner, reservation);
        QrTicket reissued = qrTicketRepository.findByReservationId(reservation.getId()).orElseThrow();
        assertThat(reissued.getKeyId()).isEqualTo(qrTokenProvider.getKeyId());
        assertThat(reissued.getRevokedAt()).isNull();
        assertThat(reissued.getRevokedReason()).isNull();

        mockMvc.perform(post("/staff/check-ins")
                .header("Authorization", bearerToken(staff))
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkInBody(currentToken)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.checkInStatus").value("CHECKED_IN"));
        mockMvc.perform(get("/actuator/health/readiness"))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("ticket request waiting on an in-flight rotation is rejected after the rotation commits")
    void ticketRequestDuringRotationIsRejected() throws Exception {
        User owner = saveUser("owner@example.com", UserRole.USER);
        Reservation reservation = persistConfirmedReservation(owner);
        qrTicketKeyService.activateCurrentKey();
        getTicketToken(owner, reservation);
        QrTicketKeyService nextKeyService = new QrTicketKeyService(
            qrTicketKeyRepository,
            qrTicketRepository,
            new QrTokenProvider("next-qr-ticket-secret-key-for-poplot-123456789")
        );
        CountDownLatch rotationApplied = new CountDownLatch(1);
        CountDownLatch releaseRotation = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> rotation = executor.submit(() -> transactionTemplate.execute(status -> {
                int revokedCount = nextKeyService.activateCurrentKey();
                rotationApplied.countDown();
                awaitQuietly(releaseRotation);
                return revokedCount;
            }));
            assertThat(rotationApplied.await(10, TimeUnit.SECONDS)).isTrue();

            Future<Integer> ticketRequest = executor.submit(() -> mockMvc.perform(
                    get("/reservations/{publicId}/ticket", reservation.getPublicId())
                        .header("Authorization", bearerToken(owner)))
                .andReturn().getResponse().getStatus());
            assertThat(waitUntilLockWaiterExists()).isTrue();
            assertThat(ticketRequest.isDone()).isFalse();
            releaseRotation.countDown();

            assertThat(rotation.get(10, TimeUnit.SECONDS)).isEqualTo(1);
            assertThat(ticketRequest.get(10, TimeUnit.SECONDS)).isEqualTo(503);
        } finally {
            releaseRotation.countDown();
            executor.shutdownNow();
        }
        QrTicket ticket = qrTicketRepository.findByReservationId(reservation.getId()).orElseThrow();
        assertThat(ticket.getKeyId()).isEqualTo(qrTokenProvider.getKeyId());
        assertThat(ticket.getRevokedAt()).isNotNull();
        assertThat(ticket.getRevokedReason()).isEqualTo(QrTicketRevocationReason.KEY_ROTATED);
    }

    @Test
    @DisplayName("ticket revoked for a reason other than key rotation is not reissued")
    void ticketRevokedForOtherReasonIsNotReissued() throws Exception {
        User owner = saveUser("owner@example.com", UserRole.USER);
        Reservation reservation = persistConfirmedReservation(owner);
        qrTicketKeyService.activateCurrentKey();
        getTicketToken(owner, reservation);
        assertThatThrownBy(() -> jdbcTemplate.update(
            "UPDATE qr_ticket SET revoked_reason = 'KEY_ROTATED' WHERE reservation_id = ?",
            reservation.getId()
        )).isInstanceOf(DataIntegrityViolationException.class);
        jdbcTemplate.update("UPDATE qr_ticket SET revoked_at = now() WHERE reservation_id = ?", reservation.getId());

        mockMvc.perform(get("/reservations/{publicId}/ticket", reservation.getPublicId())
                .header("Authorization", bearerToken(owner)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("RESERVATION_409_08"));
        assertThat(qrTicketRepository.findByReservationId(reservation.getId()).orElseThrow().getRevokedAt())
            .isNotNull();
    }

    @Test
    @DisplayName("activating the already active key is idempotent and only one key can be active")
    void activationIsIdempotentAndSingleActive() {
        qrTicketKeyService.activateCurrentKey();
        Instant activatedAt = qrTicketKeyRepository.findById(qrTokenProvider.getKeyId()).orElseThrow().getActivatedAt();

        assertThat(qrTicketKeyService.activateCurrentKey()).isZero();
        assertThat(qrTicketKeyRepository.count()).isEqualTo(1);
        assertThat(qrTicketKeyRepository.findById(qrTokenProvider.getKeyId()).orElseThrow().getActivatedAt())
            .isEqualTo(activatedAt);
        assertThatThrownBy(() -> jdbcTemplate.update(
            "INSERT INTO qr_ticket_key (key_id, status, activated_at) VALUES ('other-key', 'ACTIVE', now())"
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    private int activateWith(QrTokenProvider provider) {
        QrTicketKeyService service = new QrTicketKeyService(qrTicketKeyRepository, qrTicketRepository, provider);
        return transactionTemplate.execute(status -> service.activateCurrentKey());
    }

    private boolean waitUntilLockWaiterExists() throws InterruptedException {
        for (int attempt = 0; attempt < 40; attempt++) {
            Integer waiters = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM pg_stat_activity WHERE wait_event_type = 'Lock' AND datname = current_database()",
                Integer.class
            );
            if (waiters != null && waiters > 0) {
                return true;
            }
            Thread.sleep(50);
        }
        return false;
    }

    private void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private String getTicketToken(User owner, Reservation reservation) throws Exception {
        MvcResult result = mockMvc.perform(get("/reservations/{publicId}/ticket", reservation.getPublicId())
                .header("Authorization", bearerToken(owner)))
            .andExpect(status().isOk())
            .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.qrToken");
    }

    private Reservation persistConfirmedReservation(User owner) {
        LocalDateTime slotStart = LocalDateTime.now(RESERVATION_ZONE).truncatedTo(ChronoUnit.MINUTES);
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
            ReflectionTestUtils.setField(event, "checkInOpenMinutes", 30);
            ReflectionTestUtils.setField(event, "checkInCloseMinutes", 30);
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
            ReflectionTestUtils.setField(reservation, "status", ReservationStatus.CONFIRMED);
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

    private String bearerToken(User user) {
        return "Bearer " + jwtProvider.createAccessToken(user.getId());
    }

    private String checkInBody(String qrToken) {
        return "{\"qrToken\":\"" + qrToken + "\"}";
    }
}
