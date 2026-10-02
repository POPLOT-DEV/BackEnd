package com.poplot.feature.reservation.service;

import com.poplot.feature.event.entity.Event;
import com.poplot.feature.event.enums.EventStatus;
import com.poplot.feature.reservation.entity.TimeSlot;
import com.poplot.feature.reservation.enums.TimeSlotStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
class ReservationEligibilityPolicy {

    boolean isReservable(TimeSlot timeSlot, Instant now) {
        Event event = timeSlot.getEvent();
        return event.getStatus() == EventStatus.RESERVATION_OPEN
            && timeSlot.getStatus() == TimeSlotStatus.OPEN
            && !now.isBefore(event.getReservationOpenAt())
            && now.isBefore(event.getReservationCloseAt());
    }
}
