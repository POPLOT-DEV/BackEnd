package com.poplot.feature.reservation.service;

import com.poplot.feature.reservation.dto.response.QrTicketResponse;
import com.poplot.feature.reservation.entity.QrTicket;
import com.poplot.feature.reservation.entity.Reservation;
import com.poplot.feature.reservation.enums.ReservationStatus;
import com.poplot.feature.reservation.exception.ReservationErrorCode;
import com.poplot.feature.reservation.repository.QrTicketRepository;
import com.poplot.feature.reservation.repository.ReservationRepository;
import com.poplot.feature.reservation.token.QrTokenProvider;
import com.poplot.feature.user.entity.User;
import com.poplot.feature.user.service.CurrentUserService;
import com.poplot.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class QrTicketService {
    private static final ZoneId RESERVATION_ZONE = ZoneId.of("Asia/Seoul");

    private final ReservationRepository reservationRepository;
    private final QrTicketRepository qrTicketRepository;
    private final QrTokenProvider qrTokenProvider;
    private final CurrentUserService currentUserService;

    @Transactional(timeout = 3)
    public QrTicketResponse getTicket(String reservationPublicId) {
        Reservation reservation = getReservationForUpdate(reservationPublicId);
        validateOwner(reservation);
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new GeneralException(ReservationErrorCode.TICKET_UNAVAILABLE);
        }

        String token = qrTokenProvider.createToken(reservation.getPublicId());
        String tokenHash = qrTokenProvider.hash(token);
        Instant now = Instant.now();
        Instant expiresAt = calculateExpiresAt(reservation);
        QrTicket ticket = qrTicketRepository.findByReservationId(reservation.getId())
            .orElseGet(() -> qrTicketRepository.save(new QrTicket(reservation, tokenHash, now, expiresAt)));
        if (!ticket.getTokenHash().equals(tokenHash) || !Objects.equals(ticket.getExpiresAt(), expiresAt)) {
            ticket.reissue(tokenHash, now, expiresAt);
        }
        return QrTicketResponse.of(reservation, token);
    }

    private Reservation getReservationForUpdate(String reservationPublicId) {
        UUID publicId;
        try {
            publicId = UUID.fromString(reservationPublicId);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new GeneralException(ReservationErrorCode.RESERVATION_NOT_FOUND);
        }
        return reservationRepository.findForUpdateByPublicId(publicId)
            .orElseThrow(() -> new GeneralException(ReservationErrorCode.RESERVATION_NOT_FOUND));
    }

    private void validateOwner(Reservation reservation) {
        User currentUser = currentUserService.getCurrentUser();
        if (!reservation.getUser().getId().equals(currentUser.getId())) {
            throw new GeneralException(ReservationErrorCode.FORBIDDEN_RESERVATION);
        }
    }

    private Instant calculateExpiresAt(Reservation reservation) {
        Integer closeMinutes = reservation.getTimeSlot().getEvent().getCheckInCloseMinutes();
        if (closeMinutes == null || closeMinutes < 0) {
            return null;
        }
        return LocalDateTime.of(reservation.getTimeSlot().getDate(), reservation.getTimeSlot().getStartTime())
            .plusMinutes(closeMinutes)
            .atZone(RESERVATION_ZONE)
            .toInstant();
    }
}
