package com.poplot.feature.reservation.service;

import com.poplot.feature.event.entity.Event;
import com.poplot.feature.event.enums.EventStatus;
import com.poplot.feature.reservation.entity.TimeSlot;
import com.poplot.feature.reservation.enums.TimeSlotStatus;
import com.poplot.feature.store.entity.Store;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationEligibilityPolicyTest {
    private static final Instant OPEN_AT = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant CLOSE_AT = Instant.parse("2026-10-01T01:00:00Z");

    private final ReservationEligibilityPolicy policy = new ReservationEligibilityPolicy();

    @Test
    @DisplayName("reservation eligibility includes opening time and excludes closing time")
    void appliesReservationWindowBoundaries() {
        TimeSlot timeSlot = createTimeSlot(EventStatus.RESERVATION_OPEN, TimeSlotStatus.OPEN);

        assertThat(policy.isReservable(timeSlot, OPEN_AT)).isTrue();
        assertThat(policy.isReservable(timeSlot, CLOSE_AT.minusNanos(1))).isTrue();
        assertThat(policy.isReservable(timeSlot, CLOSE_AT)).isFalse();
    }

    @Test
    @DisplayName("reservation eligibility requires open event and time slot states")
    void requiresOpenStates() {
        TimeSlot scheduledEventSlot = createTimeSlot(EventStatus.SCHEDULED, TimeSlotStatus.OPEN);
        TimeSlot closedSlot = createTimeSlot(EventStatus.RESERVATION_OPEN, TimeSlotStatus.CLOSED);

        assertThat(policy.isReservable(scheduledEventSlot, OPEN_AT)).isFalse();
        assertThat(policy.isReservable(closedSlot, OPEN_AT)).isFalse();
    }

    private TimeSlot createTimeSlot(EventStatus eventStatus, TimeSlotStatus timeSlotStatus) {
        Event event = new Event(
            new Store("store"),
            "event",
            OPEN_AT,
            CLOSE_AT,
            LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 10, 2),
            4,
            10
        );
        ReflectionTestUtils.setField(event, "status", eventStatus);
        TimeSlot timeSlot = new TimeSlot(
            event,
            LocalDate.of(2026, 10, 1),
            LocalTime.of(10, 0),
            LocalTime.of(11, 0),
            4
        );
        ReflectionTestUtils.setField(timeSlot, "status", timeSlotStatus);
        return timeSlot;
    }
}
