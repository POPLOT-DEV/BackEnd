package com.poplot.feature.reservation.dto.response;

import com.poplot.feature.reservation.entity.Reservation;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record QrTicketResponse(
    UUID reservationPublicId,
    String qrToken,
    String eventName,
    LocalDate reservationDate,
    LocalTime startTime,
    LocalTime endTime,
    int partySize
) {
    public static QrTicketResponse of(Reservation reservation, String qrToken) {
        return new QrTicketResponse(
            reservation.getPublicId(),
            qrToken,
            reservation.getTimeSlot().getEvent().getTitle(),
            reservation.getTimeSlot().getDate(),
            reservation.getTimeSlot().getStartTime(),
            reservation.getTimeSlot().getEndTime(),
            reservation.getPartySize()
        );
    }
}
