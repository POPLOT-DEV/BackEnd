package com.poplot.feature.reservation.dto.response;

import com.poplot.feature.reservation.entity.Waitlist;
import com.poplot.feature.reservation.enums.WaitlistStatus;

import java.time.Instant;
import java.util.UUID;

public record DeclineWaitlistResponse(
    UUID waitlistPublicId,
    WaitlistStatus status,
    Instant declinedAt
) {
    public static DeclineWaitlistResponse from(Waitlist waitlist) {
        return new DeclineWaitlistResponse(
            waitlist.getPublicId(),
            waitlist.getStatus(),
            waitlist.getDeclinedAt()
        );
    }
}
