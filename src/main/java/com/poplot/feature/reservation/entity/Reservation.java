package com.poplot.feature.reservation.entity;

import com.poplot.feature.reservation.enums.ReservationStatus;
import com.poplot.feature.reservation.exception.ReservationErrorCode;
import com.poplot.feature.reservation.exception.CheckInErrorCode;
import com.poplot.feature.user.entity.User;
import com.poplot.global.common.entity.BaseTimeEntity;
import com.poplot.global.exception.GeneralException;
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
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "reservation",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_reservation_public_id", columnNames = "public_id"),
        @UniqueConstraint(name = "uk_reservation_number", columnNames = "reservation_number"),
        @UniqueConstraint(name = "uk_reservation_user_idempotency", columnNames = {"user_id", "idempotency_key"})
    }
)
public class Reservation extends BaseTimeEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "time_slot_id", nullable = false)
    private TimeSlot timeSlot;

    @Column(name = "reservation_number", nullable = false, length = 50)
    private String reservationNumber;

    @Column(name = "party_size", nullable = false)
    private int partySize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "idempotency_key", length = 255)
    private String idempotencyKey;

    @Column(name = "hold_expires_at")
    private Instant holdExpiresAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Column(name = "checked_in_at")
    private Instant checkedInAt;

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reservation_id")
    private Long id;

    public Reservation(User user, TimeSlot timeSlot, String reservationNumber, int partySize) {
        this(user, timeSlot, reservationNumber, partySize, null);
    }

    public Reservation(User user, TimeSlot timeSlot, String reservationNumber, int partySize, Instant holdExpiresAt) {
        this.user = user;
        this.timeSlot = timeSlot;
        this.reservationNumber = reservationNumber;
        this.partySize = partySize;
        this.status = ReservationStatus.PENDING;
        this.holdExpiresAt = holdExpiresAt;
        this.publicId = UUID.randomUUID();
    }

    public void confirm(Instant now) {
        if (status != ReservationStatus.PENDING) {
            throw new GeneralException(ReservationErrorCode.INVALID_CONFIRM_STATUS);
        }
        if (holdExpiresAt != null && now.isAfter(holdExpiresAt)) {
            throw new GeneralException(ReservationErrorCode.CONFIRMATION_EXPIRED);
        }
        status = ReservationStatus.CONFIRMED;
        confirmedAt = now;
    }

    public void cancel(Instant now, Instant cancellationDeadline) {
        if (status != ReservationStatus.PENDING && status != ReservationStatus.CONFIRMED) {
            throw new GeneralException(ReservationErrorCode.INVALID_CANCEL_STATUS);
        }
        if (cancellationDeadline != null && now.isAfter(cancellationDeadline)) {
            throw new GeneralException(ReservationErrorCode.CANCELLATION_EXPIRED);
        }
        status = ReservationStatus.CANCELED;
        canceledAt = now;
    }

    public void checkIn(Instant now) {
        if (status == ReservationStatus.CHECKED_IN) {
            throw new GeneralException(CheckInErrorCode.ALREADY_CHECKED_IN);
        }
        if (status != ReservationStatus.CONFIRMED) {
            throw new GeneralException(CheckInErrorCode.INVALID_RESERVATION_STATUS);
        }
        status = ReservationStatus.CHECKED_IN;
        checkedInAt = now;
    }
}
