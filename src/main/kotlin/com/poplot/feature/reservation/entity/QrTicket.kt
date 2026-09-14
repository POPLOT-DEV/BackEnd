package com.poplot.feature.reservation.entity

import com.poplot.global.common.entity.BaseCreatedTimeEntity
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "qr_ticket",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_qr_ticket_reservation", columnNames = ["reservation_id"]),
        UniqueConstraint(name = "uk_qr_ticket_token_hash", columnNames = ["token_hash"]),
    ],
)
class QrTicket(
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reservation_id", nullable = false)
    val reservation: Reservation,

    @Column(name = "token_hash", nullable = false, length = 255)
    val tokenHash: String,

    @Column(name = "issued_at", nullable = false)
    val issuedAt: Instant,

    @Column(name = "expires_at")
    var expiresAt: Instant? = null,

    @Column(name = "revoked_at")
    var revokedAt: Instant? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "qr_ticket_id")
    val id: Long? = null,
) : BaseCreatedTimeEntity()
