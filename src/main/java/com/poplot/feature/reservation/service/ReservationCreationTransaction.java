package com.poplot.feature.reservation.service;

import com.poplot.feature.event.entity.Event;
import com.poplot.feature.event.enums.EventStatus;
import com.poplot.feature.reservation.dto.request.CreateReservationRequest;
import com.poplot.feature.reservation.dto.response.CreateReservationResponse;
import com.poplot.feature.reservation.entity.Reservation;
import com.poplot.feature.reservation.entity.TimeSlot;
import com.poplot.feature.reservation.enums.ReservationStatus;
import com.poplot.feature.reservation.enums.TimeSlotStatus;
import com.poplot.feature.reservation.exception.ReservationErrorCode;
import com.poplot.feature.reservation.repository.ReservationRepository;
import com.poplot.feature.reservation.repository.TimeSlotRepository;
import com.poplot.feature.user.entity.User;
import com.poplot.feature.user.service.CurrentUserService;
import com.poplot.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.EnumSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class ReservationCreationTransaction {
    private static final EnumSet<ReservationStatus> DUPLICATE_BLOCKING_STATUSES = EnumSet.of(
        ReservationStatus.PENDING,
        ReservationStatus.CONFIRMED,
        ReservationStatus.CHECKED_IN
    );

    private final CurrentUserService currentUserService;
    private final TimeSlotRepository timeSlotRepository;
    private final ReservationRepository reservationRepository;

    @Transactional
    public CreateReservationResponse create(CreateReservationRequest request) {
        validatePartySize(request);
        TimeSlot timeSlot = getTimeSlot(request);
        Instant now = Instant.now();
        validateReservable(timeSlot, now);

        User user = currentUserService.getCurrentUser();
        boolean duplicateReservation = reservationRepository.existsByUserIdAndTimeSlotIdAndStatusIn(
            user.getId(),
            timeSlot.getId(),
            DUPLICATE_BLOCKING_STATUSES
        );
        if (duplicateReservation) {
            throw new GeneralException(ReservationErrorCode.DUPLICATE_RESERVATION);
        }
        if (request.partySize() > timeSlot.getRemainingCount()) {
            throw new GeneralException(ReservationErrorCode.INSUFFICIENT_CAPACITY);
        }

        timeSlot.reserve(request.partySize());
        Instant holdExpiresAt = now.plus(timeSlot.getEvent().getHoldMinutes(), ChronoUnit.MINUTES);
        Reservation reservation = new Reservation(
            user,
            timeSlot,
            createReservationNumber(),
            request.partySize(),
            holdExpiresAt
        );
        return CreateReservationResponse.from(reservationRepository.save(reservation));
    }

    private void validatePartySize(CreateReservationRequest request) {
        if (request == null || request.partySize() == null || request.partySize() < 1) {
            throw new GeneralException(ReservationErrorCode.INVALID_PARTY_SIZE);
        }
    }

    private TimeSlot getTimeSlot(CreateReservationRequest request) {
        if (request.timeSlotPublicId() == null || request.timeSlotPublicId().isBlank()) {
            throw new GeneralException(ReservationErrorCode.TIME_SLOT_NOT_FOUND);
        }
        UUID publicId;
        try {
            publicId = UUID.fromString(request.timeSlotPublicId());
        } catch (IllegalArgumentException exception) {
            throw new GeneralException(ReservationErrorCode.TIME_SLOT_NOT_FOUND);
        }
        return timeSlotRepository.findByPublicId(publicId)
            .orElseThrow(() -> new GeneralException(ReservationErrorCode.TIME_SLOT_NOT_FOUND));
    }

    private void validateReservable(TimeSlot timeSlot, Instant now) {
        Event event = timeSlot.getEvent();
        boolean reservationPeriodOpen = !now.isBefore(event.getReservationOpenAt())
            && now.isBefore(event.getReservationCloseAt());
        boolean unavailable = event.getStatus() != EventStatus.RESERVATION_OPEN
            || timeSlot.getStatus() != TimeSlotStatus.OPEN
            || !reservationPeriodOpen;
        if (unavailable) {
            throw new GeneralException(ReservationErrorCode.UNAVAILABLE_TIME_SLOT);
        }
    }

    private String createReservationNumber() {
        return "R-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }
}
