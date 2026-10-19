package com.poplot.feature.reservation.repository;

import com.poplot.feature.reservation.entity.QrTicketKey;
import com.poplot.feature.reservation.enums.QrTicketKeyStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface QrTicketKeyRepository extends JpaRepository<QrTicketKey, String> {
    boolean existsByKeyIdAndStatus(String keyId, QrTicketKeyStatus status);

    @Lock(LockModeType.PESSIMISTIC_READ)
    Optional<QrTicketKey> findForShareByKeyIdAndStatus(String keyId, QrTicketKeyStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<QrTicketKey> findForUpdateByStatus(QrTicketKeyStatus status);
}
