package com.poplot.feature.auth.service

import com.poplot.feature.auth.exception.AuthErrorCode
import com.poplot.feature.auth.repository.AuthSessionRepository
import com.poplot.feature.user.enums.UserStatus
import com.poplot.global.exception.GeneralException
import com.poplot.global.security.jwt.JwtProvider
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class RefreshTokenService(
    private val jwtProvider: JwtProvider,
    private val refreshTokenHasher: RefreshTokenHasher,
    private val authSessionRepository: AuthSessionRepository,
    private val authTokenService: AuthTokenService,
) {
    @Transactional(noRollbackFor = [GeneralException::class])
    fun refresh(refreshToken: String?): IssuedAuthTokens {
        if (refreshToken.isNullOrBlank()) {
            throw GeneralException(AuthErrorCode.REFRESH_TOKEN_REQUIRED)
        }

        val userId = validateAndExtractUserId(refreshToken)
        val session = authSessionRepository.findByRefreshTokenHashForUpdate(
            refreshTokenHasher.hash(refreshToken),
        ) ?: throw GeneralException(AuthErrorCode.UNUSABLE_REFRESH_TOKEN)
        val now = Instant.now()

        if (session.revokedAt != null) {
            authSessionRepository.revokeActiveSessionsByFamilyId(session.familyId, now)
            throw GeneralException(AuthErrorCode.UNUSABLE_REFRESH_TOKEN)
        }
        if (!session.expiresAt.isAfter(now)) {
            throw GeneralException(AuthErrorCode.UNUSABLE_REFRESH_TOKEN)
        }
        if (session.user.id != userId || session.user.status != UserStatus.ACTIVE) {
            throw GeneralException(AuthErrorCode.UNUSABLE_REFRESH_TOKEN)
        }

        session.revoke(now)
        return authTokenService.issueForFamily(session.user, session.familyId)
    }

    private fun validateAndExtractUserId(refreshToken: String): Long =
        try {
            if (!jwtProvider.isRefreshToken(refreshToken)) {
                throw GeneralException(AuthErrorCode.INVALID_REFRESH_TOKEN)
            }
            jwtProvider.getUserId(refreshToken)
        } catch (exception: ExpiredJwtException) {
            throw GeneralException(AuthErrorCode.EXPIRED_REFRESH_TOKEN)
        } catch (exception: JwtException) {
            throw GeneralException(AuthErrorCode.INVALID_REFRESH_TOKEN)
        } catch (exception: IllegalArgumentException) {
            throw GeneralException(AuthErrorCode.INVALID_REFRESH_TOKEN)
        }
}
