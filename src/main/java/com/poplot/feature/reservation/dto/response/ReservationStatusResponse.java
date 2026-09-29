package com.poplot.feature.reservation.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.poplot.feature.reservation.entity.Reservation;
import com.poplot.feature.reservation.enums.ReservationStatus;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReservationStatusResponse(
    UUID reservationPublicId,
    ReservationStatus status,
    Instant confirmedAt,
    Instant canceledAt
) {
    public static ReservationStatusResponse confirmed(Reservation reservation) {
        return new ReservationStatusResponse(
            reservation.getPublicId(),
            reservation.getStatus(),
            reservation.getConfirmedAt(),
            null
        );
    }

    public static ReservationStatusResponse canceled(Reservation reservation) {
        return new ReservationStatusResponse(
            reservation.getPublicId(),
            reservation.getStatus(),
            null,
            reservation.getCanceledAt()
        );
    }
}
