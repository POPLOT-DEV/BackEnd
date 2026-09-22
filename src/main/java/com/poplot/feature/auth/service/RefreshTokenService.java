package com.poplot.feature.auth.service;

import com.poplot.feature.auth.entity.AuthSession;
import com.poplot.feature.auth.exception.AuthErrorCode;
import com.poplot.feature.auth.repository.AuthSessionRepository;
import com.poplot.feature.user.enums.UserStatus;
import com.poplot.global.exception.GeneralException;
import com.poplot.global.security.jwt.JwtProvider;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
    private final JwtProvider jwtProvider;
    private final RefreshTokenHasher refreshTokenHasher;
    private final AuthSessionRepository authSessionRepository;
    private final AuthTokenService authTokenService;

    @Transactional(noRollbackFor = GeneralException.class)
    public IssuedAuthTokens refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new GeneralException(AuthErrorCode.REFRESH_TOKEN_REQUIRED);
        }
        long userId = validateAndExtractUserId(refreshToken);
        AuthSession session = authSessionRepository.findByRefreshTokenHashForUpdate(refreshTokenHasher.hash(refreshToken))
            .orElseThrow(() -> new GeneralException(AuthErrorCode.UNUSABLE_REFRESH_TOKEN));
        Instant now = Instant.now();
        if (session.getRevokedAt() != null) {
            authSessionRepository.revokeActiveSessionsByFamilyId(session.getFamilyId(), now);
            throw new GeneralException(AuthErrorCode.UNUSABLE_REFRESH_TOKEN);
        }
        if (!session.getExpiresAt().isAfter(now) || !session.getUser().getId().equals(userId) || session.getUser().getStatus() != UserStatus.ACTIVE) {
            throw new GeneralException(AuthErrorCode.UNUSABLE_REFRESH_TOKEN);
        }
        session.revoke(now);
        return authTokenService.issueForFamily(session.getUser(), session.getFamilyId());
    }

    private long validateAndExtractUserId(String refreshToken) {
        try {
            if (!jwtProvider.isRefreshToken(refreshToken)) {
                throw new GeneralException(AuthErrorCode.INVALID_REFRESH_TOKEN);
            }
            return jwtProvider.getUserId(refreshToken);
        } catch (ExpiredJwtException exception) {
            throw new GeneralException(AuthErrorCode.EXPIRED_REFRESH_TOKEN);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new GeneralException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }
    }
}
