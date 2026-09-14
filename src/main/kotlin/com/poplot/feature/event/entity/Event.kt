package com.poplot.feature.event.entity

import com.poplot.feature.store.entity.Store
import com.poplot.feature.event.enums.EventStatus
import com.poplot.global.common.entity.BaseTimeEntity
import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(
    name = "event",
    uniqueConstraints = [UniqueConstraint(name = "uk_event_public_id", columnNames = ["public_id"])],
)
class Event(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    val store: Store,

    @Column(nullable = false, length = 255)
    var title: String,

    @Column(name = "reservation_open_at", nullable = false)
    var reservationOpenAt: Instant,

    @Column(name = "reservation_close_at", nullable = false)
    var reservationCloseAt: Instant,

    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate,

    @Column(name = "end_date", nullable = false)
    var endDate: LocalDate,

    @Column(name = "max_party_size", nullable = false)
    var maxPartySize: Int,

    @Column(name = "hold_minutes", nullable = false)
    var holdMinutes: Int,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: EventStatus = EventStatus.DRAFT,

    @Column(name = "cancellation_deadline_minutes")
    var cancellationDeadlineMinutes: Int? = null,

    @Column(name = "check_in_open_minutes")
    var checkInOpenMinutes: Int? = null,

    @Column(name = "check_in_close_minutes")
    var checkInCloseMinutes: Int? = null,

    @Column(name = "public_id", nullable = false, updatable = false)
    val publicId: UUID = UUID.randomUUID(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    val id: Long? = null,
) : BaseTimeEntity()
