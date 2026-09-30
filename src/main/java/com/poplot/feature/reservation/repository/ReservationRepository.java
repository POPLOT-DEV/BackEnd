package com.poplot.feature.reservation.repository;

import com.poplot.feature.reservation.entity.Reservation;
import com.poplot.feature.reservation.enums.ReservationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    @EntityGraph(attributePaths = {"user", "timeSlot", "timeSlot.event", "timeSlot.event.store"})
    Optional<Reservation> findByPublicId(UUID publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Reservation> findForUpdateByPublicId(UUID publicId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Reservation> findForUpdateById(Long id);

    boolean existsByUserIdAndTimeSlotIdAndStatusIn(
        Long userId,
        Long timeSlotId,
        Collection<ReservationStatus> statuses
    );
}
