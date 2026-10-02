package com.poplot.feature.reservation.dto.response;

import com.poplot.feature.reservation.entity.Reservation;
import com.poplot.feature.reservation.entity.Waitlist;
import com.poplot.feature.reservation.enums.WaitlistStatus;

import java.time.Instant;
import java.util.UUID;

public record AcceptWaitlistResponse(
    UUID waitlistPublicId,
    WaitlistStatus status,
    UUID reservationPublicId,
    Instant acceptedAt
) {
    public static AcceptWaitlistResponse of(Waitlist waitlist, Reservation reservation) {
        return new AcceptWaitlistResponse(
            waitlist.getPublicId(),
            waitlist.getStatus(),
            reservation.getPublicId(),
            waitlist.getAcceptedAt()
        );
    }
}
