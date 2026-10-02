package com.poplot.feature.reservation.dto.response;

import com.poplot.feature.reservation.entity.Waitlist;
import com.poplot.feature.reservation.enums.WaitlistStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record WaitlistDetailResponse(
    UUID waitlistPublicId,
    WaitlistStatus status,
    int partySize,
    Instant createdAt,
    Instant offerExpiresAt,
    EventInfo event,
    TimeSlotInfo timeSlot
) {
    public static WaitlistDetailResponse from(Waitlist waitlist) {
        return new WaitlistDetailResponse(
            waitlist.getPublicId(),
            waitlist.getStatus(),
            waitlist.getPartySize(),
            waitlist.getCreatedAt(),
            waitlist.getOfferExpiresAt(),
            new EventInfo(
                waitlist.getTimeSlot().getEvent().getPublicId(),
                waitlist.getTimeSlot().getEvent().getTitle(),
                waitlist.getTimeSlot().getEvent().getStore().getBannerImgUrl()
            ),
            new TimeSlotInfo(
                waitlist.getTimeSlot().getPublicId(),
                waitlist.getTimeSlot().getDate(),
                waitlist.getTimeSlot().getStartTime(),
                waitlist.getTimeSlot().getEndTime()
            )
        );
    }

    public record EventInfo(UUID eventPublicId, String name, String bannerImgUrl) {
    }

    public record TimeSlotInfo(UUID timeSlotPublicId, LocalDate date, LocalTime startTime, LocalTime endTime) {
    }
}
