package com.poplot.feature.reservation.repository;

import com.poplot.feature.reservation.entity.QrTicket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QrTicketRepository extends JpaRepository<QrTicket, Long> {
    Optional<QrTicket> findByReservationId(Long reservationId);

    Optional<QrTicket> findByTokenHash(String tokenHash);
}
