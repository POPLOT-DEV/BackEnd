package com.poplot.feature.reservation.service;

import com.poplot.feature.reservation.entity.QrTicketKey;
import com.poplot.feature.reservation.enums.QrTicketKeyStatus;
import com.poplot.feature.reservation.enums.QrTicketRevocationReason;
import com.poplot.feature.reservation.repository.QrTicketKeyRepository;
import com.poplot.feature.reservation.repository.QrTicketRepository;
import com.poplot.feature.reservation.token.QrTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class QrTicketKeyService {
    private final QrTicketKeyRepository qrTicketKeyRepository;
    private final QrTicketRepository qrTicketRepository;
    private final QrTokenProvider qrTokenProvider;

    @Transactional
    public int activateCurrentKey() {
        String keyId = qrTokenProvider.getKeyId();
        Instant now = Instant.now();
        Optional<QrTicketKey> activeKey = qrTicketKeyRepository.findForUpdateByStatus(QrTicketKeyStatus.ACTIVE);
        if (activeKey.isEmpty() || !activeKey.get().getKeyId().equals(keyId)) {
            if (qrTicketKeyRepository.existsById(keyId)) {
                throw new IllegalStateException("Retired QR ticket key cannot be reactivated.");
            }
            // 부분 유니크 인덱스 위반 방지를 위한 퇴역 UPDATE 선반영
            activeKey.ifPresent(key -> {
                key.retire(now);
                qrTicketKeyRepository.flush();
            });
            qrTicketKeyRepository.save(new QrTicketKey(keyId, now));
            log.info("Activated new QR ticket key. previousKeyRetired={}", activeKey.isPresent());
        }
        int revokedCount = qrTicketRepository.revokeTicketsNotIssuedWithKey(
            keyId,
            QrTicketRevocationReason.KEY_ROTATED,
            now
        );
        log.info("Revoked QR tickets not issued with the active key. revokedCount={}", revokedCount);
        return revokedCount;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public boolean lockCurrentKeyIfActive() {
        return qrTicketKeyRepository.findForShareByKeyIdAndStatus(qrTokenProvider.getKeyId(), QrTicketKeyStatus.ACTIVE)
            .isPresent();
    }

    @Transactional(readOnly = true)
    public boolean isCurrentKeyActive() {
        return qrTicketKeyRepository.existsByKeyIdAndStatus(qrTokenProvider.getKeyId(), QrTicketKeyStatus.ACTIVE);
    }
}
