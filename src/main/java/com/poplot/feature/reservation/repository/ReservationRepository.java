package com.poplot.feature.reservation.repository;

import com.poplot.feature.reservation.entity.Reservation;
import com.poplot.feature.reservation.enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    boolean existsByUserIdAndTimeSlotIdAndStatusIn(
        Long userId,
        Long timeSlotId,
        Collection<ReservationStatus> statuses
    );
}
