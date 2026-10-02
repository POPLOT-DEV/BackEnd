package com.poplot.feature.reservation.service;

import com.poplot.feature.event.entity.Event;
import com.poplot.feature.event.enums.EventStatus;
import com.poplot.feature.reservation.entity.TimeSlot;
import com.poplot.feature.reservation.entity.Waitlist;
import com.poplot.feature.reservation.enums.WaitlistStatus;
import com.poplot.feature.reservation.repository.WaitlistRepository;
import com.poplot.feature.store.entity.Store;
import com.poplot.feature.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WaitlistOfferCoordinatorTest {

    @Test
    @DisplayName("offer expiration does not exceed reservation closing time")
    void capsOfferExpirationAtReservationClosing() {
        Instant now = Instant.parse("2026-10-01T00:00:00Z");
        Instant reservationCloseAt = now.plus(Duration.ofMinutes(5));
        TimeSlot timeSlot = createTimeSlot(now, reservationCloseAt);
        Waitlist waitlist = new Waitlist(User.create("user@example.com", "user"), timeSlot, 1);
        WaitlistRepository repository = mock(WaitlistRepository.class);
        when(repository.findFirstByTimeSlotIdAndStatusOrderByOfferedAtAscIdAsc(null, WaitlistStatus.OFFERED))
            .thenReturn(Optional.empty());
        when(repository.findFirstByTimeSlotIdAndStatusAndPartySizeLessThanEqualOrderByCreatedAtAscIdAsc(
            null,
            WaitlistStatus.WAITING,
            timeSlot.getRemainingCount()
        )).thenReturn(Optional.of(waitlist));
        WaitlistOfferCoordinator coordinator = new WaitlistOfferCoordinator(
            repository,
            Duration.ofMinutes(10),
            new ReservationEligibilityPolicy()
        );

        coordinator.offerNext(timeSlot, now);

        assertThat(waitlist.getOfferExpiresAt()).isEqualTo(reservationCloseAt);
    }

    private TimeSlot createTimeSlot(Instant now, Instant reservationCloseAt) {
        Event event = new Event(
            new Store("store"),
            "event",
            now.minusSeconds(60),
            reservationCloseAt,
            LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 10, 2),
            4,
            10
        );
        ReflectionTestUtils.setField(event, "status", EventStatus.RESERVATION_OPEN);
        return new TimeSlot(
            event,
            LocalDate.of(2026, 10, 1),
            LocalTime.of(10, 0),
            LocalTime.of(11, 0),
            4
        );
    }
}
