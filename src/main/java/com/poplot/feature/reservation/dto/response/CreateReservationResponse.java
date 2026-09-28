package com.poplot.feature.reservation.dto.response;

import com.poplot.feature.reservation.entity.Reservation;
import com.poplot.feature.reservation.enums.ReservationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record CreateReservationResponse(
    UUID reservationPublicId,
    ReservationStatus status,
    int partySize,
    Instant expiresAt,
    EventResponse event,
    TimeSlotResponse timeSlot
) {
    public static CreateReservationResponse from(Reservation reservation) {
        return new CreateReservationResponse(
            reservation.getPublicId(),
            reservation.getStatus(),
            reservation.getPartySize(),
            reservation.getHoldExpiresAt(),
            new EventResponse(
                reservation.getTimeSlot().getEvent().getPublicId(),
                reservation.getTimeSlot().getEvent().getTitle()
            ),
            new TimeSlotResponse(
                reservation.getTimeSlot().getPublicId(),
                reservation.getTimeSlot().getDate(),
                reservation.getTimeSlot().getStartTime(),
                reservation.getTimeSlot().getEndTime()
            )
        );
    }

    public record EventResponse(UUID eventPublicId, String name) {
    }

    public record TimeSlotResponse(
        UUID timeSlotPublicId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime
    ) {
    }
}
