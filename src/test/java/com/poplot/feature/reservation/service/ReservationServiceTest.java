package com.poplot.feature.reservation.service;

import com.poplot.feature.reservation.dto.request.CreateReservationRequest;
import com.poplot.feature.reservation.exception.ReservationErrorCode;
import com.poplot.feature.reservation.repository.ReservationRepository;
import com.poplot.feature.reservation.repository.TimeSlotRepository;
import com.poplot.feature.user.service.CurrentUserService;
import com.poplot.global.exception.GeneralException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.QueryTimeoutException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReservationServiceTest {
    @Test
    @DisplayName("optimistic locking conflict is translated to insufficient capacity")
    void translatesOptimisticLockingConflict() {
        ReservationCreationTransaction transaction = mock(ReservationCreationTransaction.class);
        ReservationService service = new ReservationService(
            transaction,
            mock(ReservationRepository.class),
            mock(TimeSlotRepository.class),
            mock(CurrentUserService.class),
            mock(WaitlistOfferCoordinator.class)
        );
        CreateReservationRequest request = new CreateReservationRequest(UUID.randomUUID().toString(), 1);
        when(transaction.create(request)).thenThrow(new ObjectOptimisticLockingFailureException("TimeSlot", 1L));

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ReservationErrorCode.INSUFFICIENT_CAPACITY));
    }

    @Test
    @DisplayName("pessimistic lock acquisition failure is translated to retryable reservation conflict")
    void translatesPessimisticLockingFailure() {
        ReservationCreationTransaction transaction = mock(ReservationCreationTransaction.class);
        ReservationService service = new ReservationService(
            transaction,
            mock(ReservationRepository.class),
            mock(TimeSlotRepository.class),
            mock(CurrentUserService.class),
            mock(WaitlistOfferCoordinator.class)
        );
        CreateReservationRequest request = new CreateReservationRequest(UUID.randomUUID().toString(), 1);
        when(transaction.create(request)).thenThrow(new CannotAcquireLockException("lock timeout"));

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ReservationErrorCode.LOCK_ACQUISITION_FAILED));
    }

    @Test
    @DisplayName("reservation transaction timeout is translated to retryable reservation conflict")
    void translatesReservationTransactionTimeout() {
        ReservationCreationTransaction transaction = mock(ReservationCreationTransaction.class);
        ReservationService service = new ReservationService(
            transaction,
            mock(ReservationRepository.class),
            mock(TimeSlotRepository.class),
            mock(CurrentUserService.class),
            mock(WaitlistOfferCoordinator.class)
        );
        CreateReservationRequest request = new CreateReservationRequest(UUID.randomUUID().toString(), 1);
        when(transaction.create(request)).thenThrow(new QueryTimeoutException("transaction timeout"));

        assertThatThrownBy(() -> service.create(request))
            .isInstanceOfSatisfying(GeneralException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ReservationErrorCode.LOCK_ACQUISITION_FAILED));
    }
}
