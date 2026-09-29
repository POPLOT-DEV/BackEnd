package com.poplot.feature.reservation.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.poplot.feature.event.entity.Event;
import com.poplot.feature.reservation.entity.Reservation;
import com.poplot.feature.reservation.entity.TimeSlot;
import com.poplot.feature.reservation.enums.ReservationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ReservationDetailResponse(
    UUID reservationPublicId,
    ReservationStatus status,
    int partySize,
    Instant createdAt,
    Instant expiresAt,
    EventResponse event,
    TimeSlotResponse timeSlot
) {
    public static ReservationDetailResponse from(Reservation reservation) {
        return new ReservationDetailResponse(
            reservation.getPublicId(),
            reservation.getStatus(),
            reservation.getPartySize(),
            reservation.getCreatedAt(),
            reservation.getHoldExpiresAt(),
            EventResponse.from(reservation.getTimeSlot().getEvent()),
            TimeSlotResponse.from(reservation.getTimeSlot())
        );
    }

    public record EventResponse(UUID eventPublicId, String name, String bannerImgUrl) {
        private static EventResponse from(Event event) {
            return new EventResponse(event.getPublicId(), event.getTitle(), event.getStore().getBannerImgUrl());
        }
    }

    public record TimeSlotResponse(
        UUID timeSlotPublicId,
        LocalDate date,
        @JsonFormat(pattern = "HH:mm") LocalTime startTime,
        @JsonFormat(pattern = "HH:mm") LocalTime endTime
    ) {
        private static TimeSlotResponse from(TimeSlot timeSlot) {
            return new TimeSlotResponse(
                timeSlot.getPublicId(),
                timeSlot.getDate(),
                timeSlot.getStartTime(),
                timeSlot.getEndTime()
            );
        }
    }
}
