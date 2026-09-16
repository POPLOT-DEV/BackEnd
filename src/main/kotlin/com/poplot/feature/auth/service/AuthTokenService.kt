package com.poplot.feature.auth.service

import com.poplot.feature.auth.entity.AuthSession
import com.poplot.feature.auth.repository.AuthSessionRepository
import com.poplot.feature.user.entity.User
import com.poplot.global.security.jwt.JwtProvider
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthTokenService(
    private val jwtProvider: JwtProvider,
    private val refreshTokenHasher: RefreshTokenHasher,
    private val authSessionRepository: AuthSessionRepository,
) {
    @Transactional
    fun issue(user: User): IssuedAuthTokens {
        val userId = checkNotNull(user.id) { "Cannot issue tokens for a transient user." }
        val accessToken = jwtProvider.createAccessToken(userId)
        val refreshToken = jwtProvider.createRefreshToken(userId)
        authSessionRepository.save(
            AuthSession(
                user = user,
                refreshTokenHash = refreshTokenHasher.hash(refreshToken),
                expiresAt = jwtProvider.getExpiration(refreshToken),
            ),
        )
        return IssuedAuthTokens(
            accessToken = accessToken,
            refreshToken = refreshToken,
            accessTokenExpiresIn = jwtProvider.getAccessTokenExpirationSeconds(),
        )
    }
}
