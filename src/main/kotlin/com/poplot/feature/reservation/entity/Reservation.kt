package com.poplot.feature.reservation.entity

import com.poplot.feature.user.entity.User
import com.poplot.feature.reservation.enums.ReservationStatus
import com.poplot.global.common.entity.BaseTimeEntity
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "reservation",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_reservation_public_id", columnNames = ["public_id"]),
        UniqueConstraint(name = "uk_reservation_number", columnNames = ["reservation_number"]),
        UniqueConstraint(name = "uk_reservation_user_idempotency", columnNames = ["user_id", "idempotency_key"]),
    ],
)
class Reservation(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "time_slot_id", nullable = false)
    val timeSlot: TimeSlot,

    @Column(name = "reservation_number", nullable = false, length = 50)
    val reservationNumber: String,

    @Column(name = "party_size", nullable = false)
    val partySize: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ReservationStatus = ReservationStatus.PENDING,

    @Column(name = "idempotency_key", length = 255)
    val idempotencyKey: String? = null,

    @Column(name = "hold_expires_at")
    var holdExpiresAt: Instant? = null,

    @Column(name = "confirmed_at")
    var confirmedAt: Instant? = null,

    @Column(name = "canceled_at")
    var canceledAt: Instant? = null,

    @Column(name = "checked_in_at")
    var checkedInAt: Instant? = null,

    @Column(name = "public_id", nullable = false, updatable = false)
    val publicId: UUID = UUID.randomUUID(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    val id: Long? = null,
) : BaseTimeEntity()
