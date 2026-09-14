package com.poplot.feature.reservation.entity

import com.poplot.feature.user.entity.User
import com.poplot.feature.reservation.enums.WaitlistStatus
import com.poplot.global.common.entity.BaseTimeEntity
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "waitlist",
    uniqueConstraints = [UniqueConstraint(name = "uk_waitlist_public_id", columnNames = ["public_id"])],
)
class Waitlist(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "time_slot_id", nullable = false)
    val timeSlot: TimeSlot,

    @Column(name = "party_size", nullable = false)
    val partySize: Int,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: WaitlistStatus = WaitlistStatus.WAITING,

    @Column(name = "offered_at")
    var offeredAt: Instant? = null,

    @Column(name = "offer_expires_at")
    var offerExpiresAt: Instant? = null,

    @Column(name = "accepted_at")
    var acceptedAt: Instant? = null,

    @Column(name = "canceled_at")
    var canceledAt: Instant? = null,

    @Column(name = "public_id", nullable = false, updatable = false)
    val publicId: UUID = UUID.randomUUID(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "waitlist_id")
    val id: Long? = null,
) : BaseTimeEntity()
