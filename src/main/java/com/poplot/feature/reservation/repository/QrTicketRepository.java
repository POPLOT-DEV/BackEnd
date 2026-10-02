package com.poplot.feature.reservation.repository;

import com.poplot.feature.reservation.entity.QrTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface QrTicketRepository extends JpaRepository<QrTicket, Long> {
    Optional<QrTicket> findByReservationId(Long reservationId);

    @Query("""
        select ticket.id as ticketId, ticket.reservation.id as reservationId
        from QrTicket ticket
        where ticket.tokenHash = :tokenHash
        """)
    Optional<CheckInTarget> findCheckInTargetByTokenHash(@Param("tokenHash") String tokenHash);

    interface CheckInTarget {
        Long getTicketId();

        Long getReservationId();
    }
}
