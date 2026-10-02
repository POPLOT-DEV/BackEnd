package com.poplot.feature.reservation.service;

import com.poplot.feature.reservation.enums.WaitlistStatus;
import com.poplot.feature.reservation.repository.WaitlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class WaitlistExpirationScheduler {
    private final WaitlistRepository waitlistRepository;
    private final WaitlistTransaction waitlistTransaction;

    @Scheduled(fixedDelayString = "${waitlist.expiration-scan-delay}")
    public void expireOffers() {
        waitlistRepository.findTop100ByStatusAndOfferExpiresAtBeforeOrderByOfferExpiresAtAsc(
            WaitlistStatus.OFFERED,
            Instant.now()
        ).forEach(waitlist -> expireOfferSafely(waitlist.getId()));
    }

    private void expireOfferSafely(Long waitlistId) {
        try {
            waitlistTransaction.expireOffer(waitlistId);
        } catch (RuntimeException exception) {
            log.warn("Failed to expire waitlist offer. waitlistId={}", waitlistId, exception);
        }
    }
}
