package com.poplot.feature.reservation.controller;

import com.poplot.feature.event.entity.Event;
import com.poplot.feature.event.enums.EventStatus;
import com.poplot.feature.reservation.entity.Reservation;
import com.poplot.feature.reservation.entity.TimeSlot;
import com.poplot.feature.reservation.entity.Waitlist;
import com.poplot.feature.reservation.enums.ReservationStatus;
import com.poplot.feature.reservation.enums.WaitlistStatus;
import com.poplot.feature.reservation.repository.ReservationRepository;
import com.poplot.feature.reservation.repository.TimeSlotRepository;
import com.poplot.feature.reservation.repository.WaitlistRepository;
import com.poplot.feature.reservation.service.WaitlistExpirationScheduler;
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
import java.util.List;
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
class WaitlistControllerTest {
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
    private WaitlistRepository waitlistRepository;
    @Autowired
    private WaitlistExpirationScheduler waitlistExpirationScheduler;
    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("user registers and retrieves one active waitlist entry")
    void registersAndRetrievesWaitlist() throws Exception {
        User user = saveUser("user@example.com");
        TimeSlot timeSlot = persistSoldOutTimeSlot(2, List.of(2));

        mockMvc.perform(post("/timeslots/{publicId}/waitlists", timeSlot.getPublicId())
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"partySize\":2}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.status").value("WAITING"))
            .andExpect(jsonPath("$.data.partySize").value(2))
            .andExpect(jsonPath("$.data.createdAt").isNotEmpty());

        Waitlist waitlist = waitlistRepository.findAll().getFirst();
        mockMvc.perform(get("/waitlists/{publicId}", waitlist.getPublicId())
                .header("Authorization", bearerToken(user)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.event.name").value("event"))
            .andExpect(jsonPath("$.data.timeSlot.timeSlotPublicId").value(timeSlot.getPublicId().toString()));

        mockMvc.perform(post("/timeslots/{publicId}/waitlists", timeSlot.getPublicId())
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"partySize\":1}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("WAITLIST_409_01"));
    }

    @Test
    @DisplayName("waitlist registration validates party size, availability, and ownership")
    void validatesRegistrationAndOwnership() throws Exception {
        User owner = saveUser("owner@example.com");
        User other = saveUser("other@example.com");
        TimeSlot soldOut = persistSoldOutTimeSlot(2, List.of(2));
        TimeSlot available = persistSoldOutTimeSlot(2, List.of(1));

        mockMvc.perform(post("/timeslots/{publicId}/waitlists", soldOut.getPublicId())
                .header("Authorization", bearerToken(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"partySize\":0}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("WAITLIST_400_01"));
        mockMvc.perform(post("/timeslots/{publicId}/waitlists", available.getPublicId())
                .header("Authorization", bearerToken(owner))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"partySize\":1}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("WAITLIST_409_02"));

        register(owner, soldOut, 1);
        Waitlist waitlist = waitlistRepository.findAll().getFirst();
        mockMvc.perform(get("/waitlists/{publicId}", waitlist.getPublicId())
                .header("Authorization", bearerToken(other)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("WAITLIST_403_01"));
    }

    @Test
    @DisplayName("reservation cancellation offers the next waitlist entry and acceptance creates a reservation")
    void cancellationOffersAndAcceptanceCreatesReservation() throws Exception {
        User reservationOwner = saveUser("reservation-owner@example.com");
        User waitingUser = saveUser("waiting@example.com");
        TimeSlot timeSlot = persistSoldOutTimeSlot(2, List.of());
        Reservation canceledReservation = persistReservation(reservationOwner, timeSlot, 2);
        register(waitingUser, timeSlot, 2);

        mockMvc.perform(post("/reservations/{publicId}/cancel", canceledReservation.getPublicId())
                .header("Authorization", bearerToken(reservationOwner)))
            .andExpect(status().isOk());

        Waitlist waitlist = waitlistRepository.findAll().getFirst();
        assertThat(waitlistRepository.findById(waitlist.getId()).orElseThrow().getStatus())
            .isEqualTo(WaitlistStatus.OFFERED);
        mockMvc.perform(post("/waitlists/{publicId}/accept", waitlist.getPublicId())
                .header("Authorization", bearerToken(waitingUser)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("ACCEPTED"))
            .andExpect(jsonPath("$.data.reservationPublicId").isNotEmpty())
            .andExpect(jsonPath("$.data.acceptedAt").isNotEmpty());

        TimeSlot savedTimeSlot = timeSlotRepository.findById(timeSlot.getId()).orElseThrow();
        assertThat(savedTimeSlot.getReservedCount()).isEqualTo(2);
        assertThat(reservationRepository.findAll())
            .filteredOn(reservation -> reservation.getStatus() == ReservationStatus.PENDING)
            .singleElement()
            .extracting(Reservation::getPartySize)
            .isEqualTo(2);

        mockMvc.perform(post("/waitlists/{publicId}/accept", waitlist.getPublicId())
                .header("Authorization", bearerToken(waitingUser)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("WAITLIST_409_04"));
    }

    @Test
    @DisplayName("acceptance after reservation closing is rejected without consuming capacity")
    void rejectsAcceptanceAfterReservationClosing() throws Exception {
        User waitingUser = saveUser("closed-acceptance@example.com");
        TimeSlot timeSlot = persistSoldOutTimeSlot(2, List.of(1));
        Waitlist offered = persistWaitlist(waitingUser, timeSlot, 1, WaitlistStatus.OFFERED, Instant.now().plusSeconds(600));
        closeReservationPeriod(timeSlot);

        mockMvc.perform(post("/waitlists/{publicId}/accept", offered.getPublicId())
                .header("Authorization", bearerToken(waitingUser)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("WAITLIST_400_02"));

        assertThat(timeSlotRepository.findById(timeSlot.getId()).orElseThrow().getReservedCount()).isEqualTo(1);
        assertThat(reservationRepository.findAll()).isEmpty();
        assertThat(waitlistRepository.findById(offered.getId()).orElseThrow().getStatus()).isEqualTo(WaitlistStatus.OFFERED);
    }

    @Test
    @DisplayName("cancellation after reservation closing does not create a new offer")
    void doesNotOfferAfterReservationClosing() throws Exception {
        User reservationOwner = saveUser("closed-owner@example.com");
        User waitingUser = saveUser("closed-waiting@example.com");
        TimeSlot timeSlot = persistSoldOutTimeSlot(2, List.of());
        Reservation canceledReservation = persistReservation(reservationOwner, timeSlot, 2);
        register(waitingUser, timeSlot, 1);
        closeReservationPeriod(timeSlot);

        mockMvc.perform(post("/reservations/{publicId}/cancel", canceledReservation.getPublicId())
                .header("Authorization", bearerToken(reservationOwner)))
            .andExpect(status().isOk());

        Waitlist waitlist = findWaitlist(waitingUser);
        assertThat(waitlistRepository.findById(waitlist.getId()).orElseThrow().getStatus())
            .isEqualTo(WaitlistStatus.WAITING);
    }

    @Test
    @DisplayName("offer selection skips an oversized party and decline advances to the next feasible entry")
    void selectsFeasibleCandidateAndAdvancesAfterDecline() throws Exception {
        User reservationOwner = saveUser("reservation-owner@example.com");
        User largeParty = saveUser("large@example.com");
        User firstSmallParty = saveUser("small-1@example.com");
        User secondSmallParty = saveUser("small-2@example.com");
        TimeSlot timeSlot = persistSoldOutTimeSlot(2, List.of());
        Reservation firstReservation = persistReservation(reservationOwner, timeSlot, 1);
        persistReservation(saveUser("remaining-owner@example.com"), timeSlot, 1);
        register(largeParty, timeSlot, 2);
        register(firstSmallParty, timeSlot, 1);
        register(secondSmallParty, timeSlot, 1);

        mockMvc.perform(post("/reservations/{publicId}/cancel", firstReservation.getPublicId())
                .header("Authorization", bearerToken(reservationOwner)))
            .andExpect(status().isOk());

        Waitlist large = findWaitlist(largeParty);
        Waitlist firstSmall = findWaitlist(firstSmallParty);
        Waitlist secondSmall = findWaitlist(secondSmallParty);
        assertThat(waitlistRepository.findById(large.getId()).orElseThrow().getStatus()).isEqualTo(WaitlistStatus.WAITING);
        assertThat(waitlistRepository.findById(firstSmall.getId()).orElseThrow().getStatus()).isEqualTo(WaitlistStatus.OFFERED);

        mockMvc.perform(post("/waitlists/{publicId}/decline", firstSmall.getPublicId())
                .header("Authorization", bearerToken(firstSmallParty)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status").value("DECLINED"))
            .andExpect(jsonPath("$.data.declinedAt").isNotEmpty());

        assertThat(waitlistRepository.findById(secondSmall.getId()).orElseThrow().getStatus())
            .isEqualTo(WaitlistStatus.OFFERED);
    }

    @Test
    @DisplayName("expired acceptance is committed before the next candidate receives an offer")
    void expiredAcceptanceAdvancesToNextCandidate() throws Exception {
        User expiredUser = saveUser("expired@example.com");
        User nextUser = saveUser("next@example.com");
        TimeSlot timeSlot = persistSoldOutTimeSlot(2, List.of(1));
        Waitlist expired = persistWaitlist(expiredUser, timeSlot, 1, WaitlistStatus.OFFERED, Instant.now().minusSeconds(1));
        Waitlist next = persistWaitlist(nextUser, timeSlot, 1, WaitlistStatus.WAITING, null);

        mockMvc.perform(post("/waitlists/{publicId}/accept", expired.getPublicId())
                .header("Authorization", bearerToken(expiredUser)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("WAITLIST_409_03"));

        assertThat(waitlistRepository.findById(expired.getId()).orElseThrow().getStatus()).isEqualTo(WaitlistStatus.EXPIRED);
        assertThat(waitlistRepository.findById(next.getId()).orElseThrow().getStatus()).isEqualTo(WaitlistStatus.OFFERED);
    }

    @Test
    @DisplayName("expiration scheduler expires an offer and advances to the next candidate")
    void expirationSchedulerAdvancesToNextCandidate() {
        User expiredUser = saveUser("scheduler-expired@example.com");
        User nextUser = saveUser("scheduler-next@example.com");
        TimeSlot timeSlot = persistSoldOutTimeSlot(2, List.of(1));
        Waitlist expired = persistWaitlist(expiredUser, timeSlot, 1, WaitlistStatus.OFFERED, Instant.now().minusSeconds(1));
        Waitlist next = persistWaitlist(nextUser, timeSlot, 1, WaitlistStatus.WAITING, null);

        waitlistExpirationScheduler.expireOffers();

        assertThat(waitlistRepository.findById(expired.getId()).orElseThrow().getStatus()).isEqualTo(WaitlistStatus.EXPIRED);
        assertThat(waitlistRepository.findById(next.getId()).orElseThrow().getStatus()).isEqualTo(WaitlistStatus.OFFERED);
    }

    private void register(User user, TimeSlot timeSlot, int partySize) throws Exception {
        mockMvc.perform(post("/timeslots/{publicId}/waitlists", timeSlot.getPublicId())
                .header("Authorization", bearerToken(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"partySize\":" + partySize + "}"))
            .andExpect(status().isCreated());
    }

    private TimeSlot persistSoldOutTimeSlot(int capacity, List<Integer> reservedParties) {
        return transactionTemplate.execute(status -> {
            Store store = new Store("store-" + UUID.randomUUID());
            ReflectionTestUtils.setField(store, "bannerImgUrl", "https://example.com/banner.jpg");
            entityManager.persist(store);
            Event event = new Event(
                store,
                "event",
                Instant.now().minusSeconds(3_600),
                Instant.now().plusSeconds(3_600),
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(1),
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
            reservedParties.forEach(timeSlot::reserve);
            entityManager.persist(timeSlot);
            entityManager.flush();
            return timeSlot;
        });
    }

    private Reservation persistReservation(User user, TimeSlot timeSlot, int partySize) {
        return transactionTemplate.execute(status -> {
            TimeSlot managedTimeSlot = entityManager.find(TimeSlot.class, timeSlot.getId());
            managedTimeSlot.reserve(partySize);
            Reservation reservation = new Reservation(user, managedTimeSlot, "R-" + UUID.randomUUID(), partySize);
            ReflectionTestUtils.setField(reservation, "status", ReservationStatus.CONFIRMED);
            entityManager.persist(reservation);
            entityManager.flush();
            return reservation;
        });
    }

    private Waitlist persistWaitlist(
        User user,
        TimeSlot timeSlot,
        int partySize,
        WaitlistStatus status,
        Instant expiresAt
    ) {
        return transactionTemplate.execute(transactionStatus -> {
            Waitlist waitlist = new Waitlist(user, entityManager.find(TimeSlot.class, timeSlot.getId()), partySize);
            ReflectionTestUtils.setField(waitlist, "status", status);
            if (status == WaitlistStatus.OFFERED) {
                ReflectionTestUtils.setField(waitlist, "offeredAt", Instant.now().minusSeconds(60));
                ReflectionTestUtils.setField(waitlist, "offerExpiresAt", expiresAt);
            }
            entityManager.persist(waitlist);
            entityManager.flush();
            return waitlist;
        });
    }

    private Waitlist findWaitlist(User user) {
        return waitlistRepository.findAll().stream()
            .filter(waitlist -> waitlist.getUser().getId().equals(user.getId()))
            .findFirst()
            .orElseThrow();
    }

    private void closeReservationPeriod(TimeSlot timeSlot) {
        transactionTemplate.executeWithoutResult(status -> entityManager.createQuery("""
                update Event event
                set event.reservationCloseAt = :closedAt
                where event.id = :eventId
                """)
            .setParameter("closedAt", Instant.now().minusSeconds(1))
            .setParameter("eventId", timeSlot.getEvent().getId())
            .executeUpdate());
        entityManager.clear();
    }

    private User saveUser(String email) {
        return userRepository.save(User.create(email, email));
    }

    private String bearerToken(User user) {
        return "Bearer " + jwtProvider.createAccessToken(user.getId());
    }
}
