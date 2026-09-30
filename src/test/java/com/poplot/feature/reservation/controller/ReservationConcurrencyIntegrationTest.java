package com.poplot.feature.reservation.controller;

import com.poplot.feature.event.entity.Event;
import com.poplot.feature.event.enums.EventStatus;
import com.poplot.feature.reservation.entity.Reservation;
import com.poplot.feature.reservation.entity.QrTicket;
import com.poplot.feature.reservation.entity.TimeSlot;
import com.poplot.feature.reservation.enums.ReservationStatus;
import com.poplot.feature.reservation.repository.QrTicketRepository;
import com.poplot.feature.reservation.repository.ReservationRepository;
import com.poplot.feature.reservation.repository.TimeSlotRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@ActiveProfiles("test")
@SpringBootTest(properties = {"spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=validate"})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
@Sql(statements = {
    "DELETE FROM qr_ticket",
    "DELETE FROM reservation",
    "DELETE FROM time_slot",
    "DELETE FROM event",
    "DELETE FROM store",
    "DELETE FROM users"
})
class ReservationConcurrencyIntegrationTest {
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
    private TimeSlotRepository timeSlotRepository;
    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private QrTokenProvider qrTokenProvider;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("concurrent reservation requests never exceed time slot capacity")
    void concurrentRequestsDoNotExceedCapacity() throws Exception {
        int capacity = 10;
        TimeSlot timeSlot = persistTimeSlot(capacity);
        List<User> users = saveUsers(20);

        List<Integer> statuses = runConcurrently(users.stream()
            .map(user -> createRequest(user, timeSlot, 1))
            .toList());

        assertThat(statuses).filteredOn(status -> status == 201).hasSize(capacity);
        assertThat(statuses).filteredOn(status -> status == 409).hasSize(users.size() - capacity);
        assertCapacityMatchesReservations(timeSlot.getId(), capacity);
    }

    @Test
    @DisplayName("concurrent cancellation and reservation preserve capacity consistency")
    void concurrentCancellationAndCreationPreserveConsistency() throws Exception {
        int capacity = 4;
        TimeSlot timeSlot = persistTimeSlot(capacity);
        List<User> users = saveUsers(2);
        User cancelingUser = users.get(0);
        User creatingUser = users.get(1);

        assertThat(performCreate(cancelingUser, timeSlot, capacity)).isEqualTo(201);
        Reservation reservation = reservationRepository.findAll().getFirst();

        List<Integer> statuses = runConcurrently(List.of(
            () -> mockMvc.perform(post("/reservations/{reservationPublicId}/cancel", reservation.getPublicId())
                    .header("Authorization", bearerToken(cancelingUser)))
                .andReturn().getResponse().getStatus(),
            createRequest(creatingUser, timeSlot, capacity)
        ));

        assertThat(statuses.getFirst()).isEqualTo(200);
        assertThat(statuses.get(1)).isIn(201, 409);
        assertCapacityMatchesReservations(timeSlot.getId(), capacity);
    }

    @Test
    @DisplayName("reservation creation returns a conflict when the time slot lock wait times out")
    void lockWaitTimeoutReturnsConflict() throws Exception {
        TimeSlot timeSlot = persistTimeSlot(1);
        User user = saveUsers(1).getFirst();
        CountDownLatch lockAcquired = new CountDownLatch(1);
        CountDownLatch releaseLock = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> lockHolder = executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
            timeSlotRepository.findForUpdateById(timeSlot.getId()).orElseThrow();
            lockAcquired.countDown();
            try {
                if (!releaseLock.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out while holding the lock");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
        }));

        try {
            assertThat(lockAcquired.await(5, TimeUnit.SECONDS)).isTrue();
            long startedAt = System.nanoTime();

            MvcResult result = mockMvc.perform(post("/reservations")
                    .header("Authorization", bearerToken(user))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"timeSlotPublicId\":\"" + timeSlot.getPublicId() + "\",\"partySize\":1}"))
                .andReturn();

            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
            assertThat(result.getResponse().getStatus()).isEqualTo(409);
            assertThat(result.getResponse().getContentAsString()).contains("RESERVATION_409_07");
            assertThat(elapsedMillis).isBetween(2_000L, 8_000L);
            assertCapacityMatchesReservations(timeSlot.getId(), 1);
        } finally {
            releaseLock.countDown();
            lockHolder.get(5, TimeUnit.SECONDS);
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("two concurrent scans check in a reservation exactly once")
    void concurrentScansCheckInExactlyOnce() throws Exception {
        User owner = userRepository.save(User.create("owner@example.com", "owner"));
        User staff = userRepository.save(User.builder()
            .email("staff@example.com")
            .name("staff")
            .role(UserRole.STAFF)
            .status(UserStatus.ACTIVE)
            .build());
        Reservation reservation = persistConfirmedReservationForCurrentCheckIn(owner);
        String qrToken = qrTokenProvider.createToken(reservation.getPublicId());
        transactionTemplate.executeWithoutResult(status -> qrTicketRepository.save(
            new QrTicket(reservationRepository.findById(reservation.getId()).orElseThrow(), qrTokenProvider.hash(qrToken), Instant.now())
        ));
        Callable<Integer> checkInRequest = () -> mockMvc.perform(post("/staff/check-ins")
                .header("Authorization", bearerToken(staff))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"qrToken\":\"" + qrToken + "\"}"))
            .andReturn().getResponse().getStatus();

        List<Integer> statuses = runConcurrently(List.of(checkInRequest, checkInRequest));

        assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        Reservation saved = reservationRepository.findById(reservation.getId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(ReservationStatus.CHECKED_IN);
        assertThat(saved.getCheckedInAt()).isNotNull();
    }

    private List<Integer> runConcurrently(List<Callable<Integer>> requests) throws Exception {
        CountDownLatch ready = new CountDownLatch(requests.size());
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(requests.size());
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (Callable<Integer> request : requests) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    if (!start.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("Timed out while waiting to start");
                    }
                    return request.call();
                }));
            }
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get(20, TimeUnit.SECONDS));
            }
            return statuses;
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private Callable<Integer> createRequest(User user, TimeSlot timeSlot, int partySize) {
        return () -> performCreate(user, timeSlot, partySize);
    }

    private int performCreate(User user, TimeSlot timeSlot, int partySize) throws Exception {
        MvcResult result = mockMvc.perform(post("/reservations")
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"timeSlotPublicId\":\"" + timeSlot.getPublicId() + "\",\"partySize\":" + partySize + "}"))
            .andReturn();
        return result.getResponse().getStatus();
    }

    private void assertCapacityMatchesReservations(Long timeSlotId, int capacity) {
        Integer reservedCount = jdbcTemplate.queryForObject(
            "SELECT reserved_count FROM time_slot WHERE time_slot_id = ?",
            Integer.class,
            timeSlotId
        );
        Integer activePartySize = jdbcTemplate.queryForObject(
            "SELECT COALESCE(SUM(party_size), 0) FROM reservation WHERE time_slot_id = ? AND status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN')",
            Integer.class,
            timeSlotId
        );
        assertThat(reservedCount).isLessThanOrEqualTo(capacity);
        assertThat(reservedCount).isEqualTo(activePartySize);
    }

    private List<User> saveUsers(int count) {
        List<User> users = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            users.add(User.create("concurrency-" + index + "@example.com", "user-" + index));
        }
        return userRepository.saveAll(users);
    }

    private TimeSlot persistTimeSlot(int capacity) {
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
                capacity,
                10
            );
            ReflectionTestUtils.setField(event, "status", EventStatus.RESERVATION_OPEN);
            entityManager.persist(event);
            TimeSlot timeSlot = new TimeSlot(
                event,
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                capacity
            );
            entityManager.persist(timeSlot);
            entityManager.flush();
            return timeSlot;
        });
    }

    private Reservation persistConfirmedReservationForCurrentCheckIn(User owner) {
        return transactionTemplate.execute(status -> {
            Store store = new Store("check-in-store");
            entityManager.persist(store);
            LocalDateTime slotStart = LocalDateTime.now(ZoneId.of("Asia/Seoul")).truncatedTo(ChronoUnit.MINUTES);
            Event event = new Event(
                store,
                "check-in-event",
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
                4
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

    private String bearerToken(User user) {
        return "Bearer " + jwtProvider.createAccessToken(user.getId());
    }
}
