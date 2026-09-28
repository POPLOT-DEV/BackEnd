package com.poplot.feature.event.service;

import com.poplot.feature.event.entity.Event;
import com.poplot.feature.event.enums.EventStatus;
import com.poplot.feature.reservation.entity.TimeSlot;
import com.poplot.feature.reservation.enums.TimeSlotDisplayStatus;
import com.poplot.feature.reservation.enums.TimeSlotStatus;
import com.poplot.feature.store.entity.Store;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class TimeSlotAvailabilityCalculatorTest {
    private static final Instant NOW = Instant.parse("2026-10-01T00:00:00Z");

    private final TimeSlotAvailabilityCalculator calculator = new TimeSlotAvailabilityCalculator();

    @Test
    @DisplayName("open slots are classified by their remaining capacity")
    void classifiesOpenSlotsByRemainingCapacity() {
        Event event = createEvent(EventStatus.RESERVATION_OPEN);
        TimeSlot available = createTimeSlot(event, 20, 10, TimeSlotStatus.OPEN);
        TimeSlot lowStock = createTimeSlot(event, 20, 16, TimeSlotStatus.OPEN);
        TimeSlot soldOut = createTimeSlot(event, 20, 20, TimeSlotStatus.OPEN);

        assertThat(calculator.calculate(event, available, NOW)).isEqualTo(TimeSlotDisplayStatus.AVAILABLE);
        assertThat(calculator.calculate(event, lowStock, NOW)).isEqualTo(TimeSlotDisplayStatus.LOW_STOCK);
        assertThat(calculator.calculate(event, soldOut, NOW)).isEqualTo(TimeSlotDisplayStatus.SOLD_OUT);
    }

    @Test
    @DisplayName("event policy and slot state close otherwise reservable slots")
    void closesSlotsWhenEventOrSlotIsClosed() {
        Event scheduledEvent = createEvent(EventStatus.SCHEDULED);
        TimeSlot openSlot = createTimeSlot(scheduledEvent, 20, 0, TimeSlotStatus.OPEN);
        Event openEvent = createEvent(EventStatus.RESERVATION_OPEN);
        TimeSlot closedSlot = createTimeSlot(openEvent, 20, 0, TimeSlotStatus.CLOSED);

        assertThat(calculator.calculate(scheduledEvent, openSlot, NOW)).isEqualTo(TimeSlotDisplayStatus.CLOSED);
        assertThat(calculator.calculate(openEvent, closedSlot, NOW)).isEqualTo(TimeSlotDisplayStatus.CLOSED);
    }

    private Event createEvent(EventStatus status) {
        Event event = new Event(new Store("store"), "event", NOW.minusSeconds(60), NOW.plusSeconds(60), LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 10, 2), 4, 10);
        ReflectionTestUtils.setField(event, "status", status);
        return event;
    }

    private TimeSlot createTimeSlot(Event event, int capacity, int reservedCount, TimeSlotStatus status) {
        TimeSlot timeSlot = new TimeSlot(event, LocalDate.of(2026, 10, 1), LocalTime.of(10, 0), LocalTime.of(11, 0), capacity);
        ReflectionTestUtils.setField(timeSlot, "reservedCount", reservedCount);
        ReflectionTestUtils.setField(timeSlot, "status", status);
        return timeSlot;
    }
}
