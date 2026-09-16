package com.poplot.global.security.jwt

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class JwtProviderTest {
    private val jwtProvider = JwtProvider(
        secret = "test-jwt-secret-key-for-poplot-backend-1234567890",
        accessTokenExpiration = 3_600_000,
        refreshTokenExpiration = 1_209_600_000,
    )

    @Test
    fun `access and refresh tokens contain the expected type and user id`() {
        val accessToken = jwtProvider.createAccessToken(1L)
        val refreshToken = jwtProvider.createRefreshToken(1L)

        assertThat(jwtProvider.getUserId(accessToken)).isEqualTo(1L)
        assertThat(jwtProvider.isAccessToken(accessToken)).isTrue()
        assertThat(jwtProvider.isRefreshToken(refreshToken)).isTrue()
        assertThat(jwtProvider.getAccessTokenExpirationSeconds()).isEqualTo(3_600L)
    }

    @Test
    fun `tokens issued to the same user are unique`() {
        val first = jwtProvider.createRefreshToken(1L)
        val second = jwtProvider.createRefreshToken(1L)

        assertThat(second).isNotEqualTo(first)
        assertThat(jwtProvider.parseClaims(first).id)
            .isNotEqualTo(jwtProvider.parseClaims(second).id)
    }
}
