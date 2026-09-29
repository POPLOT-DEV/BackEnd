package com.poplot.feature.reservation.service;

import com.poplot.feature.reservation.dto.request.CreateReservationRequest;
import com.poplot.feature.reservation.dto.response.CreateReservationResponse;
import com.poplot.feature.reservation.exception.ReservationErrorCode;
import com.poplot.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final ReservationCreationTransaction reservationCreationTransaction;

    public CreateReservationResponse create(CreateReservationRequest request) {
        try {
            return reservationCreationTransaction.create(request);
        } catch (OptimisticLockingFailureException exception) {
            throw new GeneralException(ReservationErrorCode.INSUFFICIENT_CAPACITY);
        }
    }
}
