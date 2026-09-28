package com.poplot.feature.event.dto.response;

import com.poplot.feature.reservation.entity.TimeSlot;
import com.poplot.feature.reservation.enums.TimeSlotDisplayStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record TimeSlotsResponse(LocalDate date, List<TimeSlotResponse> timeSlots) {
    public record TimeSlotResponse(
        UUID timeSlotPublicId,
        LocalTime startTime,
        LocalTime endTime,
        int capacity,
        int remainingCount,
        TimeSlotDisplayStatus status
    ) {
        public static TimeSlotResponse of(TimeSlot timeSlot, TimeSlotDisplayStatus status) {
            return new TimeSlotResponse(timeSlot.getPublicId(), timeSlot.getStartTime(), timeSlot.getEndTime(), timeSlot.getCapacity(),
                Math.max(timeSlot.getRemainingCount(), 0), status);
        }
    }
}
