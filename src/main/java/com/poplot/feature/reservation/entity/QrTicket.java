package com.poplot.feature.reservation.entity;

import com.poplot.feature.reservation.enums.QrTicketRevocationReason;
import com.poplot.global.common.entity.BaseCreatedTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "qr_ticket",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_qr_ticket_reservation", columnNames = "reservation_id"),
        @UniqueConstraint(name = "uk_qr_ticket_token_hash", columnNames = "token_hash")
    }
)
public class QrTicket extends BaseCreatedTimeEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    private Reservation reservation;

    @Column(name = "token_hash", nullable = false, length = 255)
    private String tokenHash;

    @Column(name = "key_id", length = 64)
    private String keyId;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "revoked_reason", length = 30)
    private QrTicketRevocationReason revokedReason;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "qr_ticket_id")
    private Long id;

    public QrTicket(Reservation reservation, String tokenHash, String keyId, Instant issuedAt) {
        this(reservation, tokenHash, keyId, issuedAt, null);
    }

    public QrTicket(Reservation reservation, String tokenHash, String keyId, Instant issuedAt, Instant expiresAt) {
        this.reservation = reservation;
        this.tokenHash = tokenHash;
        this.keyId = keyId;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    public void reissue(String tokenHash, String keyId, Instant issuedAt, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.keyId = keyId;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.revokedAt = null;
        this.revokedReason = null;
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isReissuable() {
        return !isRevoked() || revokedReason == QrTicketRevocationReason.KEY_ROTATED;
    }
}
