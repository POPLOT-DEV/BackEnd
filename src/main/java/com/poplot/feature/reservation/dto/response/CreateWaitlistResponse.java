package com.poplot.feature.reservation.dto.response;

import com.poplot.feature.reservation.entity.Waitlist;
import com.poplot.feature.reservation.enums.WaitlistStatus;

import java.time.Instant;
import java.util.UUID;

public record CreateWaitlistResponse(
    UUID waitlistPublicId,
    WaitlistStatus status,
    int partySize,
    Instant createdAt
) {
    public static CreateWaitlistResponse from(Waitlist waitlist) {
        return new CreateWaitlistResponse(
            waitlist.getPublicId(),
            waitlist.getStatus(),
            waitlist.getPartySize(),
            waitlist.getCreatedAt()
        );
    }
}
