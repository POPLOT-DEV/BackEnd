package com.poplot.feature.reservation.repository;

import com.poplot.feature.reservation.entity.QrTicket;
import com.poplot.feature.reservation.enums.QrTicketRevocationReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface QrTicketRepository extends JpaRepository<QrTicket, Long> {
    Optional<QrTicket> findByReservationId(Long reservationId);

    @Query("""
        select ticket.id as ticketId, ticket.reservation.id as reservationId
        from QrTicket ticket
        where ticket.tokenHash = :tokenHash
        """)
    Optional<CheckInTarget> findCheckInTargetByTokenHash(@Param("tokenHash") String tokenHash);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        update QrTicket ticket
        set ticket.revokedAt = :revokedAt, ticket.revokedReason = :reason
        where ticket.revokedAt is null
          and (ticket.keyId is null or ticket.keyId <> :keyId)
        """)
    int revokeTicketsNotIssuedWithKey(
        @Param("keyId") String keyId,
        @Param("reason") QrTicketRevocationReason reason,
        @Param("revokedAt") Instant revokedAt
    );

    interface CheckInTarget {
        Long getTicketId();

        Long getReservationId();
    }
}
