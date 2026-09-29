package com.poplot.feature.reservation.service;

import com.poplot.feature.reservation.dto.request.CreateReservationRequest;
import com.poplot.feature.reservation.dto.response.CreateReservationResponse;
import com.poplot.feature.reservation.dto.response.ReservationDetailResponse;
import com.poplot.feature.reservation.dto.response.ReservationStatusResponse;
import com.poplot.feature.reservation.entity.Reservation;
import com.poplot.feature.reservation.exception.ReservationErrorCode;
import com.poplot.feature.reservation.repository.ReservationRepository;
import com.poplot.feature.user.entity.User;
import com.poplot.feature.user.service.CurrentUserService;
import com.poplot.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private static final ZoneId RESERVATION_ZONE = ZoneId.of("Asia/Seoul");

    private final ReservationCreationTransaction reservationCreationTransaction;
    private final ReservationRepository reservationRepository;
    private final CurrentUserService currentUserService;

    public CreateReservationResponse create(CreateReservationRequest request) {
        try {
            return reservationCreationTransaction.create(request);
        } catch (OptimisticLockingFailureException exception) {
            throw new GeneralException(ReservationErrorCode.INSUFFICIENT_CAPACITY);
        }
    }

    @Transactional(readOnly = true)
    public ReservationDetailResponse getDetail(String reservationPublicId) {
        return ReservationDetailResponse.from(getOwnedReservation(reservationPublicId));
    }

    @Transactional
    public ReservationStatusResponse confirm(String reservationPublicId) {
        Reservation reservation = getOwnedReservation(reservationPublicId);
        reservation.confirm(Instant.now());
        return ReservationStatusResponse.confirmed(reservation);
    }

    @Transactional
    public ReservationStatusResponse cancel(String reservationPublicId) {
        Reservation reservation = getOwnedReservation(reservationPublicId);
        Instant now = Instant.now();
        reservation.cancel(now, calculateCancellationDeadline(reservation));
        reservation.getTimeSlot().release(reservation.getPartySize());
        return ReservationStatusResponse.canceled(reservation);
    }

    private Reservation getOwnedReservation(String reservationPublicId) {
        UUID publicId;
        try {
            publicId = UUID.fromString(reservationPublicId);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new GeneralException(ReservationErrorCode.RESERVATION_NOT_FOUND);
        }
        Reservation reservation = reservationRepository.findByPublicId(publicId)
            .orElseThrow(() -> new GeneralException(ReservationErrorCode.RESERVATION_NOT_FOUND));
        User currentUser = currentUserService.getCurrentUser();
        if (!reservation.getUser().getId().equals(currentUser.getId())) {
            throw new GeneralException(ReservationErrorCode.FORBIDDEN_RESERVATION);
        }
        return reservation;
    }

    private Instant calculateCancellationDeadline(Reservation reservation) {
        Integer deadlineMinutes = reservation.getTimeSlot().getEvent().getCancellationDeadlineMinutes();
        if (deadlineMinutes == null) {
            return null;
        }
        LocalDateTime startAt = LocalDateTime.of(
            reservation.getTimeSlot().getDate(),
            reservation.getTimeSlot().getStartTime()
        );
        return startAt.minusMinutes(deadlineMinutes).atZone(RESERVATION_ZONE).toInstant();
    }
}
