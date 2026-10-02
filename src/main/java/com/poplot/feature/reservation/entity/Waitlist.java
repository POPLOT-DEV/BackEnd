package com.poplot.feature.reservation.entity;

import com.poplot.feature.reservation.enums.WaitlistStatus;
import com.poplot.feature.user.entity.User;
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
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "waitlist",
    uniqueConstraints = @UniqueConstraint(name = "uk_waitlist_public_id", columnNames = "public_id")
)
public class Waitlist extends BaseTimeEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "time_slot_id", nullable = false)
    private TimeSlot timeSlot;

    @Column(name = "party_size", nullable = false)
    private int partySize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WaitlistStatus status;

    @Column(name = "offered_at")
    private Instant offeredAt;

    @Column(name = "offer_expires_at")
    private Instant offerExpiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "declined_at")
    private Instant declinedAt;

    @Column(name = "public_id", nullable = false, updatable = false)
    private UUID publicId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "waitlist_id")
    private Long id;

    public Waitlist(User user, TimeSlot timeSlot, int partySize) {
        this.user = user;
        this.timeSlot = timeSlot;
        this.partySize = partySize;
        this.status = WaitlistStatus.WAITING;
        this.publicId = UUID.randomUUID();
    }

    public void offer(Instant now, Instant expiresAt) {
        if (status != WaitlistStatus.WAITING) {
            throw new IllegalStateException("Only a waiting entry can be offered.");
        }
        status = WaitlistStatus.OFFERED;
        offeredAt = now;
        offerExpiresAt = expiresAt;
    }

    public void accept(Instant now) {
        if (status != WaitlistStatus.OFFERED) {
            throw new IllegalStateException("Only an offered entry can be accepted.");
        }
        status = WaitlistStatus.ACCEPTED;
        acceptedAt = now;
    }

    public void decline(Instant now) {
        if (status != WaitlistStatus.OFFERED) {
            throw new IllegalStateException("Only an offered entry can be declined.");
        }
        status = WaitlistStatus.DECLINED;
        declinedAt = now;
    }

    public void expire() {
        if (status != WaitlistStatus.OFFERED) {
            throw new IllegalStateException("Only an offered entry can expire.");
        }
        status = WaitlistStatus.EXPIRED;
    }
}
