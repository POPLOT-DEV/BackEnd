package com.poplot.feature.reservation.controller;

import com.poplot.feature.event.entity.Event;
import com.poplot.feature.event.enums.EventStatus;
import com.poplot.feature.reservation.entity.Reservation;
import com.poplot.feature.reservation.entity.TimeSlot;
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
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class ReservationRollbackIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TimeSlotRepository timeSlotRepository;
    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @MockitoSpyBean
    private ReservationRepository reservationRepository;

    @Test
    @DisplayName("reservation persistence failure rolls back capacity and version changes")
    void persistenceFailureRollsBackCapacityAndVersion() throws Exception {
        User user = userRepository.save(User.create("user@example.com", "user"));
        TimeSlot timeSlot = persistTimeSlot();
        long initialVersion = timeSlot.getVersion();
        AtomicInteger flushedReservedCount = new AtomicInteger(-1);
        AtomicInteger flushedReservationCount = new AtomicInteger(-1);
        AtomicLong flushedVersion = new AtomicLong(-1);
        SimpleJpaRepository<Reservation, Long> repositoryDelegate =
            new SimpleJpaRepository<>(Reservation.class, entityManager);
        doAnswer(invocation -> {
                Reservation reservation = invocation.getArgument(0);
                repositoryDelegate.save(reservation);
                entityManager.flush();
                flushedReservedCount.set(reservation.getTimeSlot().getReservedCount());
                flushedVersion.set(reservation.getTimeSlot().getVersion());
                Number reservationCount = (Number) entityManager.createNativeQuery("SELECT COUNT(*) FROM reservation")
                    .getSingleResult();
                flushedReservationCount.set(reservationCount.intValue());
                throw new IllegalStateException("forced reservation persistence failure");
            })
            .when(reservationRepository)
            .save(any(Reservation.class));

        mockMvc.perform(post("/reservations")
                .header("Authorization", "Bearer " + jwtProvider.createAccessToken(user.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"timeSlotPublicId\":\"" + timeSlot.getPublicId() + "\",\"partySize\":2}"))
            .andExpect(status().isInternalServerError());

        TimeSlot savedTimeSlot = timeSlotRepository.findById(timeSlot.getId()).orElseThrow();
        Integer reservationCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reservation", Integer.class);
        assertThat(flushedReservedCount).hasValue(2);
        assertThat(flushedVersion).hasValue(initialVersion + 1);
        assertThat(flushedReservationCount).hasValue(1);
        assertThat(savedTimeSlot.getReservedCount()).isZero();
        assertThat(savedTimeSlot.getVersion()).isEqualTo(initialVersion);
        assertThat(reservationCount).isZero();
    }

    private TimeSlot persistTimeSlot() {
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
            ReflectionTestUtils.setField(event, "status", EventStatus.RESERVATION_OPEN);
            entityManager.persist(event);
            TimeSlot timeSlot = new TimeSlot(
                event,
                LocalDate.now().plusDays(1),
                LocalTime.of(10, 0),
                LocalTime.of(11, 0),
                10
            );
            entityManager.persist(timeSlot);
            entityManager.flush();
            return timeSlot;
        });
    }
}
