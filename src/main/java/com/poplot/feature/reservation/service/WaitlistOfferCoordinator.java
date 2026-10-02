package com.poplot.feature.reservation.service;

import com.poplot.feature.reservation.entity.TimeSlot;
import com.poplot.feature.reservation.entity.Waitlist;
import com.poplot.feature.reservation.enums.WaitlistStatus;
import com.poplot.feature.reservation.repository.WaitlistRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
public class WaitlistOfferCoordinator {
    private final WaitlistRepository waitlistRepository;
    private final Duration offerDuration;

    public WaitlistOfferCoordinator(
        WaitlistRepository waitlistRepository,
        @Value("${waitlist.offer-duration}") Duration offerDuration
    ) {
        if (offerDuration.isZero() || offerDuration.isNegative()) {
            throw new IllegalArgumentException("Waitlist offer duration must be positive.");
        }
        this.waitlistRepository = waitlistRepository;
        this.offerDuration = offerDuration;
    }

    public void offerNext(TimeSlot timeSlot, Instant now) {
        while (true) {
            var activeOffer = waitlistRepository.findFirstByTimeSlotIdAndStatusOrderByOfferedAtAscIdAsc(
                timeSlot.getId(),
                WaitlistStatus.OFFERED
            );
            if (activeOffer.isEmpty()) {
                break;
            }
            Waitlist offeredWaitlist = activeOffer.get();
            if (offeredWaitlist.getOfferExpiresAt() != null && !now.isBefore(offeredWaitlist.getOfferExpiresAt())) {
                offeredWaitlist.expire();
                continue;
            }
            return;
        }

        if (timeSlot.getRemainingCount() < 1) {
            return;
        }
        waitlistRepository
            .findFirstByTimeSlotIdAndStatusAndPartySizeLessThanEqualOrderByCreatedAtAscIdAsc(
                timeSlot.getId(),
                WaitlistStatus.WAITING,
                timeSlot.getRemainingCount()
            )
            .ifPresent(waitlist -> waitlist.offer(now, now.plus(offerDuration)));
    }
}
