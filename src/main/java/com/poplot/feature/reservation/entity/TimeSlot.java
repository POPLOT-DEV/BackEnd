package com.poplot.feature.reservation.entity;

import com.poplot.feature.event.entity.Event;
import com.poplot.feature.reservation.enums.TimeSlotStatus;
import com.poplot.global.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "time_slot",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_time_slot_public_id", columnNames = "public_id"),
        @UniqueConstraint(name = "uk_time_slot_schedule", columnNames = {"event_id", "date", "start_time", "end_time"})
    }
)
public class TimeSlot extends BaseTimeEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private int capacity;

    @Column(name = "reserved_count", nullable = false)
    private int reservedCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TimeSlotStatus status;

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId;

    @Version
    @Column(nullable = false)
    private long version;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "time_slot_id")
    private Long id;

    public TimeSlot(Event event, LocalDate date, LocalTime startTime, LocalTime endTime, int capacity) {
        this.event = event;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.capacity = capacity;
        this.status = TimeSlotStatus.OPEN;
        this.publicId = UUID.randomUUID();
    }

    public int getRemainingCount() {
        return capacity - reservedCount;
    }

    public boolean isSoldOut() {
        return getRemainingCount() <= 0;
    }
}
