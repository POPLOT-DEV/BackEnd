package com.poplot.feature.reservation.service;

import com.poplot.feature.event.entity.Event;
import com.poplot.feature.event.enums.EventStatus;
import com.poplot.feature.reservation.dto.request.CreateWaitlistRequest;
import com.poplot.feature.reservation.dto.response.AcceptWaitlistResponse;
import com.poplot.feature.reservation.dto.response.CreateWaitlistResponse;
import com.poplot.feature.reservation.dto.response.DeclineWaitlistResponse;
import com.poplot.feature.reservation.entity.Reservation;
import com.poplot.feature.reservation.entity.TimeSlot;
import com.poplot.feature.reservation.entity.Waitlist;
import com.poplot.feature.reservation.enums.ReservationStatus;
import com.poplot.feature.reservation.enums.TimeSlotStatus;
import com.poplot.feature.reservation.enums.WaitlistStatus;
import com.poplot.feature.reservation.exception.ReservationErrorCode;
import com.poplot.feature.reservation.exception.WaitlistErrorCode;
import com.poplot.feature.reservation.repository.ReservationRepository;
import com.poplot.feature.reservation.repository.TimeSlotRepository;
import com.poplot.feature.reservation.repository.WaitlistRepository;
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
class WaitlistTransaction {
    private static final EnumSet<WaitlistStatus> ACTIVE_WAITLIST_STATUSES = EnumSet.of(
        WaitlistStatus.WAITING,
        WaitlistStatus.OFFERED
    );
    private static final EnumSet<ReservationStatus> ACTIVE_RESERVATION_STATUSES = EnumSet.of(
        ReservationStatus.PENDING,
        ReservationStatus.CONFIRMED,
        ReservationStatus.CHECKED_IN
    );

    private final CurrentUserService currentUserService;
    private final TimeSlotRepository timeSlotRepository;
    private final WaitlistRepository waitlistRepository;
    private final ReservationRepository reservationRepository;
    private final WaitlistOfferCoordinator offerCoordinator;

    @Transactional(timeout = 3)
    public CreateWaitlistResponse register(String timeSlotPublicId, CreateWaitlistRequest request) {
        validatePartySize(request);
        TimeSlot timeSlot = getTimeSlotForUpdate(timeSlotPublicId);
        validateWaitlistRegistration(timeSlot, request.partySize(), Instant.now());
        User user = currentUserService.getCurrentUser();
        if (waitlistRepository.existsByUserIdAndTimeSlotIdAndStatusIn(
            user.getId(), timeSlot.getId(), ACTIVE_WAITLIST_STATUSES
        )) {
            throw new GeneralException(WaitlistErrorCode.DUPLICATE_WAITLIST);
        }
        if (reservationRepository.existsByUserIdAndTimeSlotIdAndStatusIn(
            user.getId(), timeSlot.getId(), ACTIVE_RESERVATION_STATUSES
        )) {
            throw new GeneralException(WaitlistErrorCode.UNAVAILABLE_TIME_SLOT);
        }
        return CreateWaitlistResponse.from(waitlistRepository.save(new Waitlist(user, timeSlot, request.partySize())));
    }

    @Transactional(timeout = 3)
    public AcceptResult accept(String waitlistPublicId) {
        LockedWaitlist locked = getOwnedWaitlistForUpdate(waitlistPublicId);
        Waitlist waitlist = locked.waitlist();
        Instant now = Instant.now();
        if (waitlist.getStatus() != WaitlistStatus.OFFERED) {
            throw new GeneralException(WaitlistErrorCode.INVALID_ACCEPT_STATUS);
        }
        if (isExpired(waitlist, now)) {
            waitlist.expire();
            offerCoordinator.offerNext(locked.timeSlot(), now);
            return AcceptResult.failure(WaitlistErrorCode.OFFER_EXPIRED);
        }
        if (waitlist.getPartySize() > locked.timeSlot().getRemainingCount()) {
            return AcceptResult.failure(WaitlistErrorCode.INSUFFICIENT_CAPACITY);
        }
        if (reservationRepository.existsByUserIdAndTimeSlotIdAndStatusIn(
            waitlist.getUser().getId(), locked.timeSlot().getId(), ACTIVE_RESERVATION_STATUSES
        )) {
            throw new GeneralException(WaitlistErrorCode.INVALID_ACCEPT_STATUS);
        }

        locked.timeSlot().reserve(waitlist.getPartySize());
        Reservation reservation = reservationRepository.save(new Reservation(
            waitlist.getUser(),
            locked.timeSlot(),
            createReservationNumber(),
            waitlist.getPartySize(),
            now.plus(locked.timeSlot().getEvent().getHoldMinutes(), ChronoUnit.MINUTES)
        ));
        waitlist.accept(now);
        offerCoordinator.offerNext(locked.timeSlot(), now);
        return AcceptResult.success(AcceptWaitlistResponse.of(waitlist, reservation));
    }

    @Transactional(timeout = 3)
    public DeclineResult decline(String waitlistPublicId) {
        LockedWaitlist locked = getOwnedWaitlistForUpdate(waitlistPublicId);
        Waitlist waitlist = locked.waitlist();
        Instant now = Instant.now();
        if (waitlist.getStatus() != WaitlistStatus.OFFERED) {
            throw new GeneralException(WaitlistErrorCode.INVALID_DECLINE_STATUS);
        }
        if (isExpired(waitlist, now)) {
            waitlist.expire();
            offerCoordinator.offerNext(locked.timeSlot(), now);
            return DeclineResult.failure(WaitlistErrorCode.OFFER_EXPIRED);
        }
        waitlist.decline(now);
        offerCoordinator.offerNext(locked.timeSlot(), now);
        return DeclineResult.success(DeclineWaitlistResponse.from(waitlist));
    }

    @Transactional(timeout = 3)
    public void expireOffer(Long waitlistId) {
        Long timeSlotId = waitlistRepository.findTimeSlotIdById(waitlistId).orElse(null);
        if (timeSlotId == null) {
            return;
        }
        TimeSlot timeSlot = timeSlotRepository.findForUpdateById(timeSlotId).orElse(null);
        if (timeSlot == null) {
            return;
        }
        Waitlist waitlist = waitlistRepository.findForUpdateById(waitlistId).orElse(null);
        Instant now = Instant.now();
        if (waitlist == null || waitlist.getStatus() != WaitlistStatus.OFFERED || !isExpired(waitlist, now)) {
            return;
        }
        waitlist.expire();
        offerCoordinator.offerNext(timeSlot, now);
    }

    private LockedWaitlist getOwnedWaitlistForUpdate(String waitlistPublicId) {
        UUID publicId = parseWaitlistPublicId(waitlistPublicId);
        var lockTarget = waitlistRepository.findLockTargetByPublicId(publicId)
            .orElseThrow(() -> new GeneralException(WaitlistErrorCode.NOT_FOUND));
        User currentUser = currentUserService.getCurrentUser();
        if (!lockTarget.getUserId().equals(currentUser.getId())) {
            throw new GeneralException(WaitlistErrorCode.FORBIDDEN_PROCESS);
        }
        TimeSlot timeSlot = timeSlotRepository.findForUpdateById(lockTarget.getTimeSlotId())
            .orElseThrow(() -> new GeneralException(ReservationErrorCode.TIME_SLOT_NOT_FOUND));
        Waitlist waitlist = waitlistRepository.findForUpdateByPublicId(publicId)
            .orElseThrow(() -> new GeneralException(WaitlistErrorCode.NOT_FOUND));
        if (!waitlist.getUser().getId().equals(currentUser.getId())) {
            throw new GeneralException(WaitlistErrorCode.FORBIDDEN_PROCESS);
        }
        return new LockedWaitlist(timeSlot, waitlist);
    }

    private TimeSlot getTimeSlotForUpdate(String timeSlotPublicId) {
        UUID publicId;
        try {
            publicId = UUID.fromString(timeSlotPublicId);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new GeneralException(ReservationErrorCode.TIME_SLOT_NOT_FOUND);
        }
        return timeSlotRepository.findForUpdateByPublicId(publicId)
            .orElseThrow(() -> new GeneralException(ReservationErrorCode.TIME_SLOT_NOT_FOUND));
    }

    private UUID parseWaitlistPublicId(String waitlistPublicId) {
        try {
            return UUID.fromString(waitlistPublicId);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new GeneralException(WaitlistErrorCode.NOT_FOUND);
        }
    }

    private void validatePartySize(CreateWaitlistRequest request) {
        if (request == null || request.partySize() == null || request.partySize() < 1) {
            throw new GeneralException(WaitlistErrorCode.INVALID_PARTY_SIZE);
        }
    }

    private void validateWaitlistRegistration(TimeSlot timeSlot, int partySize, Instant now) {
        Event event = timeSlot.getEvent();
        boolean reservationPeriodOpen = !now.isBefore(event.getReservationOpenAt())
            && now.isBefore(event.getReservationCloseAt());
        boolean unavailable = event.getStatus() != EventStatus.RESERVATION_OPEN
            || timeSlot.getStatus() != TimeSlotStatus.OPEN
            || !reservationPeriodOpen
            || partySize > timeSlot.getCapacity();
        if (unavailable) {
            throw new GeneralException(WaitlistErrorCode.UNAVAILABLE_TIME_SLOT);
        }
        if (timeSlot.getRemainingCount() > 0) {
            throw new GeneralException(WaitlistErrorCode.RESERVATION_AVAILABLE);
        }
    }

    private boolean isExpired(Waitlist waitlist, Instant now) {
        return waitlist.getOfferExpiresAt() == null || !now.isBefore(waitlist.getOfferExpiresAt());
    }

    private String createReservationNumber() {
        return "R-" + UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

    record AcceptResult(AcceptWaitlistResponse response, WaitlistErrorCode errorCode) {
        static AcceptResult success(AcceptWaitlistResponse response) {
            return new AcceptResult(response, null);
        }

        static AcceptResult failure(WaitlistErrorCode errorCode) {
            return new AcceptResult(null, errorCode);
        }
    }

    record DeclineResult(DeclineWaitlistResponse response, WaitlistErrorCode errorCode) {
        static DeclineResult success(DeclineWaitlistResponse response) {
            return new DeclineResult(response, null);
        }

        static DeclineResult failure(WaitlistErrorCode errorCode) {
            return new DeclineResult(null, errorCode);
        }
    }

    private record LockedWaitlist(TimeSlot timeSlot, Waitlist waitlist) {
    }
}
