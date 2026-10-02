package com.poplot.feature.reservation.token;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QrTokenProviderTest {

    @Test
    @DisplayName("different QR secrets produce different credentials and key identifiers")
    void separatesCredentialsBySecret() {
        UUID reservationPublicId = UUID.randomUUID();
        QrTokenProvider first = new QrTokenProvider("first-qr-ticket-secret-key-1234567890");
        QrTokenProvider second = new QrTokenProvider("second-qr-ticket-secret-key-123456789");

        String firstToken = first.createToken(reservationPublicId);
        String secondToken = second.createToken(reservationPublicId);

        assertThat(secondToken).isNotEqualTo(firstToken);
        assertThat(second.getKeyId()).isNotEqualTo(first.getKeyId());
        assertThat(first.matches(firstToken, first.hash(firstToken))).isTrue();
    }

    @Test
    @DisplayName("QR secret shorter than 32 bytes is rejected")
    void rejectsShortSecret() {
        assertThatThrownBy(() -> new QrTokenProvider("short-secret"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("QR ticket secret must be at least 32 bytes.");
    }
}
