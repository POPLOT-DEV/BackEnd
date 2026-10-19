package com.poplot.feature.reservation.service;

import com.poplot.feature.reservation.dto.request.CheckInRequest;
import com.poplot.feature.reservation.dto.response.CheckInResponse;
import com.poplot.feature.reservation.entity.QrTicket;
import com.poplot.feature.reservation.entity.Reservation;
import com.poplot.feature.reservation.enums.ReservationStatus;
import com.poplot.feature.reservation.exception.CheckInErrorCode;
import com.poplot.feature.reservation.repository.QrTicketRepository;
import com.poplot.feature.reservation.repository.ReservationRepository;
import com.poplot.feature.reservation.token.QrTokenProvider;
import com.poplot.feature.user.entity.User;
import com.poplot.feature.user.enums.UserRole;
import com.poplot.feature.user.service.CurrentUserService;
import com.poplot.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class CheckInService {
    private static final ZoneId RESERVATION_ZONE = ZoneId.of("Asia/Seoul");

    private final CurrentUserService currentUserService;
    private final QrTicketRepository qrTicketRepository;
    private final ReservationRepository reservationRepository;
    private final QrTokenProvider qrTokenProvider;
    private final QrTicketKeyService qrTicketKeyService;

    @Transactional(timeout = 3)
    public CheckInResponse checkIn(CheckInRequest request) {
        String token = validateRequest(request);
        validateStaffRole();
        if (!qrTicketKeyService.lockCurrentKeyIfActive()) {
            throw new GeneralException(CheckInErrorCode.TICKET_KEY_UNAVAILABLE);
        }
        String tokenHash = qrTokenProvider.hash(token);
        var target = qrTicketRepository.findCheckInTargetByTokenHash(tokenHash)
            .orElseThrow(() -> new GeneralException(CheckInErrorCode.INVALID_QR_TOKEN));
        Reservation reservation = reservationRepository.findForUpdateById(target.getReservationId())
            .orElseThrow(() -> new GeneralException(CheckInErrorCode.INVALID_QR_TOKEN));
        QrTicket ticket = qrTicketRepository.findByReservationId(reservation.getId())
            .orElseThrow(() -> new GeneralException(CheckInErrorCode.INVALID_QR_TOKEN));
        validateCurrentTicket(ticket, target.getTicketId(), token);
        Instant now = Instant.now();
        validateReservationStatus(reservation);
        validateCheckInWindow(reservation, ticket, now);
        reservation.checkIn(now);
        return CheckInResponse.from(reservation);
    }

    private void validateCurrentTicket(QrTicket ticket, Long expectedTicketId, String token) {
        boolean invalid = !ticket.getId().equals(expectedTicketId)
            || !qrTokenProvider.matches(token, ticket.getTokenHash())
            || !qrTokenProvider.getKeyId().equals(ticket.getKeyId())
            || ticket.isRevoked();
        if (invalid) {
            throw new GeneralException(CheckInErrorCode.INVALID_QR_TOKEN);
        }
    }

    private void validateReservationStatus(Reservation reservation) {
        if (reservation.getStatus() == ReservationStatus.CHECKED_IN) {
            throw new GeneralException(CheckInErrorCode.ALREADY_CHECKED_IN);
        }
        if (reservation.getStatus() != ReservationStatus.CONFIRMED) {
            throw new GeneralException(CheckInErrorCode.INVALID_RESERVATION_STATUS);
        }
    }

    private String validateRequest(CheckInRequest request) {
        if (request == null || request.qrToken() == null || request.qrToken().isBlank()) {
            throw new GeneralException(CheckInErrorCode.QR_TOKEN_REQUIRED);
        }
        return request.qrToken();
    }

    private void validateStaffRole() {
        User currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() != UserRole.STAFF && currentUser.getRole() != UserRole.ADMIN) {
            throw new GeneralException(CheckInErrorCode.FORBIDDEN);
        }
    }

    private void validateCheckInWindow(Reservation reservation, QrTicket ticket, Instant now) {
        Integer openMinutes = reservation.getTimeSlot().getEvent().getCheckInOpenMinutes();
        Integer closeMinutes = reservation.getTimeSlot().getEvent().getCheckInCloseMinutes();
        if (openMinutes == null || closeMinutes == null || openMinutes < 0 || closeMinutes < 0) {
            throw new GeneralException(CheckInErrorCode.OUTSIDE_CHECK_IN_WINDOW);
        }
        Instant startAt = LocalDateTime.of(reservation.getTimeSlot().getDate(), reservation.getTimeSlot().getStartTime())
            .atZone(RESERVATION_ZONE)
            .toInstant();
        Instant opensAt = startAt.minusSeconds(openMinutes * 60L);
        Instant closesAt = startAt.plusSeconds(closeMinutes * 60L);
        boolean expiredTicket = ticket.getExpiresAt() != null && now.isAfter(ticket.getExpiresAt());
        if (expiredTicket || now.isBefore(opensAt) || now.isAfter(closesAt)) {
            throw new GeneralException(CheckInErrorCode.OUTSIDE_CHECK_IN_WINDOW);
        }
    }
}
