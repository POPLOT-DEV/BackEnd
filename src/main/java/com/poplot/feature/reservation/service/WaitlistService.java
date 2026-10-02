package com.poplot.feature.reservation.service;

import com.poplot.feature.reservation.dto.request.CreateWaitlistRequest;
import com.poplot.feature.reservation.dto.response.AcceptWaitlistResponse;
import com.poplot.feature.reservation.dto.response.CreateWaitlistResponse;
import com.poplot.feature.reservation.dto.response.DeclineWaitlistResponse;
import com.poplot.feature.reservation.dto.response.WaitlistDetailResponse;
import com.poplot.feature.reservation.entity.Waitlist;
import com.poplot.feature.reservation.exception.WaitlistErrorCode;
import com.poplot.feature.reservation.repository.WaitlistRepository;
import com.poplot.feature.user.entity.User;
import com.poplot.feature.user.service.CurrentUserService;
import com.poplot.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WaitlistService {
    private final WaitlistTransaction waitlistTransaction;
    private final WaitlistRepository waitlistRepository;
    private final CurrentUserService currentUserService;

    public CreateWaitlistResponse register(String timeSlotPublicId, CreateWaitlistRequest request) {
        return waitlistTransaction.register(timeSlotPublicId, request);
    }

    @Transactional(readOnly = true)
    public WaitlistDetailResponse getDetail(String waitlistPublicId) {
        Waitlist waitlist = waitlistRepository.findByPublicId(parseWaitlistPublicId(waitlistPublicId))
            .orElseThrow(() -> new GeneralException(WaitlistErrorCode.NOT_FOUND));
        validateOwner(waitlist);
        return WaitlistDetailResponse.from(waitlist);
    }

    public AcceptWaitlistResponse accept(String waitlistPublicId) {
        WaitlistTransaction.AcceptResult result = waitlistTransaction.accept(waitlistPublicId);
        if (result.errorCode() != null) {
            throw new GeneralException(result.errorCode());
        }
        return result.response();
    }

    public DeclineWaitlistResponse decline(String waitlistPublicId) {
        WaitlistTransaction.DeclineResult result = waitlistTransaction.decline(waitlistPublicId);
        if (result.errorCode() != null) {
            throw new GeneralException(result.errorCode());
        }
        return result.response();
    }

    private UUID parseWaitlistPublicId(String waitlistPublicId) {
        try {
            return UUID.fromString(waitlistPublicId);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new GeneralException(WaitlistErrorCode.NOT_FOUND);
        }
    }

    private void validateOwner(Waitlist waitlist) {
        User currentUser = currentUserService.getCurrentUser();
        if (!waitlist.getUser().getId().equals(currentUser.getId())) {
            throw new GeneralException(WaitlistErrorCode.FORBIDDEN_VIEW);
        }
    }
}
