package com.poplot.feature.event.service;

import com.poplot.feature.event.entity.Event;
import com.poplot.feature.event.enums.EventStatus;
import com.poplot.feature.reservation.entity.TimeSlot;
import com.poplot.feature.reservation.enums.TimeSlotDisplayStatus;
import com.poplot.feature.reservation.enums.TimeSlotStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class TimeSlotAvailabilityCalculator {
    public TimeSlotDisplayStatus calculate(Event event, TimeSlot timeSlot, Instant now) {
        if (!isReservationOpen(event, now) || timeSlot.getStatus() != TimeSlotStatus.OPEN) {
            return TimeSlotDisplayStatus.CLOSED;
        }
        if (timeSlot.isSoldOut()) {
            return TimeSlotDisplayStatus.SOLD_OUT;
        }
        if (timeSlot.getRemainingCount() <= event.getMaxPartySize()) {
            return TimeSlotDisplayStatus.LOW_STOCK;
        }
        return TimeSlotDisplayStatus.AVAILABLE;
    }

    private boolean isReservationOpen(Event event, Instant now) {
        return event.getStatus() == EventStatus.RESERVATION_OPEN
            && !now.isBefore(event.getReservationOpenAt())
            && now.isBefore(event.getReservationCloseAt());
    }
}
