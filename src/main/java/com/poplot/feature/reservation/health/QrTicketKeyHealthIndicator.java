package com.poplot.feature.reservation.health;

import com.poplot.feature.reservation.service.QrTicketKeyService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class QrTicketKeyHealthIndicator implements HealthIndicator {
    private final QrTicketKeyService qrTicketKeyService;

    @Override
    public Health health() {
        return qrTicketKeyService.isCurrentKeyActive() ? Health.up().build() : Health.down().build();
    }
}
