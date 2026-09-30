package com.poplot.feature.reservation.dto.response;

import com.poplot.feature.reservation.entity.Reservation;
import com.poplot.feature.reservation.enums.ReservationStatus;

import java.time.Instant;
import java.util.UUID;

public record CheckInResponse(
    UUID reservationPublicId,
    ReservationStatus checkInStatus,
    Instant checkedInAt,
    int partySize
) {
    public static CheckInResponse from(Reservation reservation) {
        return new CheckInResponse(
            reservation.getPublicId(),
            reservation.getStatus(),
            reservation.getCheckedInAt(),
            reservation.getPartySize()
        );
    }
}
