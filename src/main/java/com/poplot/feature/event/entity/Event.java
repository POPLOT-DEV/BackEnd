package com.poplot.feature.event.entity;

import com.poplot.feature.event.enums.EventStatus;
import com.poplot.feature.store.entity.Store;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "event",
    uniqueConstraints = @UniqueConstraint(name = "uk_event_public_id", columnNames = "public_id")
)
public class Event extends BaseTimeEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "reservation_open_at", nullable = false)
    private Instant reservationOpenAt;

    @Column(name = "reservation_close_at", nullable = false)
    private Instant reservationCloseAt;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "max_party_size", nullable = false)
    private int maxPartySize;

    @Column(name = "hold_minutes", nullable = false)
    private int holdMinutes;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EventStatus status;

    @Column(name = "cancellation_deadline_minutes")
    private Integer cancellationDeadlineMinutes;

    @Column(name = "check_in_open_minutes")
    private Integer checkInOpenMinutes;

    @Column(name = "check_in_close_minutes")
    private Integer checkInCloseMinutes;

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "event_id")
    private Long id;

    public Event(Store store, String title, Instant reservationOpenAt, Instant reservationCloseAt, LocalDate startDate, LocalDate endDate, int maxPartySize, int holdMinutes) {
        this.store = store;
        this.title = title;
        this.reservationOpenAt = reservationOpenAt;
        this.reservationCloseAt = reservationCloseAt;
        this.startDate = startDate;
        this.endDate = endDate;
        this.maxPartySize = maxPartySize;
        this.holdMinutes = holdMinutes;
        this.status = EventStatus.DRAFT;
        this.publicId = UUID.randomUUID();
    }
}
