package com.poplot.feature.reservation.repository;

import com.poplot.feature.reservation.entity.Waitlist;
import com.poplot.feature.reservation.enums.WaitlistStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WaitlistRepository extends JpaRepository<Waitlist, Long> {
    @EntityGraph(attributePaths = {"user", "timeSlot", "timeSlot.event", "timeSlot.event.store"})
    Optional<Waitlist> findByPublicId(UUID publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Waitlist> findForUpdateByPublicId(UUID publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Waitlist> findForUpdateById(Long id);

    @Query("select waitlist.timeSlot.id from Waitlist waitlist where waitlist.id = :id")
    Optional<Long> findTimeSlotIdById(@Param("id") Long id);

    @Query("""
        select waitlist.user.id as userId, waitlist.timeSlot.id as timeSlotId
        from Waitlist waitlist
        where waitlist.publicId = :publicId
        """)
    Optional<WaitlistLockTarget> findLockTargetByPublicId(@Param("publicId") UUID publicId);

    boolean existsByUserIdAndTimeSlotIdAndStatusIn(
        Long userId,
        Long timeSlotId,
        Collection<WaitlistStatus> statuses
    );

    Optional<Waitlist> findFirstByTimeSlotIdAndStatusOrderByOfferedAtAscIdAsc(
        Long timeSlotId,
        WaitlistStatus status
    );

    Optional<Waitlist> findFirstByTimeSlotIdAndStatusAndPartySizeLessThanEqualOrderByCreatedAtAscIdAsc(
        Long timeSlotId,
        WaitlistStatus status,
        int partySize
    );

    List<Waitlist> findTop100ByStatusAndOfferExpiresAtBeforeOrderByOfferExpiresAtAsc(
        WaitlistStatus status,
        Instant now
    );

    interface WaitlistLockTarget {
        Long getUserId();

        Long getTimeSlotId();
    }
}
