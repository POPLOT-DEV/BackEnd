package com.poplot.feature.reservation.entity;

import com.poplot.feature.reservation.enums.QrTicketKeyStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "qr_ticket_key")
public class QrTicketKey {

    @Id
    @Column(name = "key_id", length = 64)
    private String keyId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QrTicketKeyStatus status;

    @Column(name = "activated_at", nullable = false)
    private Instant activatedAt;

    @Column(name = "retired_at")
    private Instant retiredAt;

    public QrTicketKey(String keyId, Instant activatedAt) {
        this.keyId = keyId;
        this.status = QrTicketKeyStatus.ACTIVE;
        this.activatedAt = activatedAt;
    }

    public void retire(Instant retiredAt) {
        this.status = QrTicketKeyStatus.RETIRED;
        this.retiredAt = retiredAt;
    }
}
