package com.poplot.feature.reservation.entity

import com.poplot.feature.event.entity.Event
import com.poplot.feature.reservation.enums.TimeSlotStatus
import com.poplot.global.common.entity.BaseTimeEntity
import jakarta.persistence.*
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Entity
@Table(
    name = "time_slot",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_time_slot_public_id", columnNames = ["public_id"]),
        UniqueConstraint(name = "uk_time_slot_schedule", columnNames = ["event_id", "date", "start_time", "end_time"]),
    ],
)
class TimeSlot(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    val event: Event,

    @Column(nullable = false)
    val date: LocalDate,

    @Column(name = "start_time", nullable = false)
    val startTime: LocalTime,

    @Column(name = "end_time", nullable = false)
    val endTime: LocalTime,

    @Column(nullable = false)
    var capacity: Int,

    @Column(name = "reserved_count", nullable = false)
    var reservedCount: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: TimeSlotStatus = TimeSlotStatus.OPEN,

    @Column(name = "public_id", nullable = false, updatable = false)
    val publicId: UUID = UUID.randomUUID(),

    @Version
    @Column(nullable = false)
    var version: Long = 0,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "time_slot_id")
    val id: Long? = null,
) : BaseTimeEntity() {
    val remainingCount: Int
        get() = capacity - reservedCount

    val isSoldOut: Boolean
        get() = remainingCount <= 0
}
