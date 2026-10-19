package com.poplot.feature.reservation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnBooleanProperty("qr-ticket.key-activation.enabled")
public class QrTicketKeyActivationRunner implements ApplicationRunner {
    private final QrTicketKeyService qrTicketKeyService;
    private final ConfigurableApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) {
        qrTicketKeyService.activateCurrentKey();
        System.exit(SpringApplication.exit(applicationContext));
    }
}
