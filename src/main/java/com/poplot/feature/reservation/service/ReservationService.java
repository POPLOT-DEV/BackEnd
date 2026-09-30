package com.poplot.feature.reservation.service;

import com.poplot.feature.reservation.dto.request.CreateReservationRequest;
import com.poplot.feature.reservation.dto.response.CreateReservationResponse;
import com.poplot.feature.reservation.dto.response.ReservationDetailResponse;
import com.poplot.feature.reservation.dto.response.ReservationStatusResponse;
import com.poplot.feature.reservation.entity.Reservation;
import com.poplot.feature.reservation.entity.TimeSlot;
import com.poplot.feature.reservation.exception.ReservationErrorCode;
import com.poplot.feature.reservation.repository.ReservationRepository;
import com.poplot.feature.reservation.repository.TimeSlotRepository;
import com.poplot.feature.user.entity.User;
import com.poplot.feature.user.service.CurrentUserService;
import com.poplot.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
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
    private final TimeSlotRepository timeSlotRepository;
    private final CurrentUserService currentUserService;

    public CreateReservationResponse create(CreateReservationRequest request) {
        try {
            return reservationCreationTransaction.create(request);
        } catch (PessimisticLockingFailureException | QueryTimeoutException exception) {
            throw new GeneralException(ReservationErrorCode.LOCK_ACQUISITION_FAILED);
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
        Reservation reservation = getOwnedReservationForUpdate(reservationPublicId);
        reservation.confirm(Instant.now());
        return ReservationStatusResponse.confirmed(reservation);
    }

    @Transactional(timeout = 3)
    public ReservationStatusResponse cancel(String reservationPublicId) {
        Reservation reservation = getOwnedReservationForUpdate(reservationPublicId);
        var lockedTimeSlot = getTimeSlotForUpdate(reservation.getTimeSlot().getId());
        Instant now = Instant.now();
        reservation.cancel(now, calculateCancellationDeadline(reservation));
        lockedTimeSlot.release(reservation.getPartySize());
        return ReservationStatusResponse.canceled(reservation);
    }

    private TimeSlot getTimeSlotForUpdate(Long timeSlotId) {
        try {
            return timeSlotRepository.findForUpdateById(timeSlotId)
                .orElseThrow(() -> new GeneralException(ReservationErrorCode.TIME_SLOT_NOT_FOUND));
        } catch (PessimisticLockingFailureException | QueryTimeoutException exception) {
            throw new GeneralException(ReservationErrorCode.LOCK_ACQUISITION_FAILED);
        }
    }

    private Reservation getOwnedReservation(String reservationPublicId) {
        UUID publicId = parseReservationPublicId(reservationPublicId);
        Reservation reservation = reservationRepository.findByPublicId(publicId)
            .orElseThrow(() -> new GeneralException(ReservationErrorCode.RESERVATION_NOT_FOUND));
        validateOwner(reservation);
        return reservation;
    }

    private Reservation getOwnedReservationForUpdate(String reservationPublicId) {
        UUID publicId = parseReservationPublicId(reservationPublicId);
        Reservation reservation = reservationRepository.findForUpdateByPublicId(publicId)
            .orElseThrow(() -> new GeneralException(ReservationErrorCode.RESERVATION_NOT_FOUND));
        validateOwner(reservation);
        return reservation;
    }

    private UUID parseReservationPublicId(String reservationPublicId) {
        UUID publicId;
        try {
            publicId = UUID.fromString(reservationPublicId);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new GeneralException(ReservationErrorCode.RESERVATION_NOT_FOUND);
        }
        return publicId;
    }

    private void validateOwner(Reservation reservation) {
        User currentUser = currentUserService.getCurrentUser();
        if (!reservation.getUser().getId().equals(currentUser.getId())) {
            throw new GeneralException(ReservationErrorCode.FORBIDDEN_RESERVATION);
        }
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
