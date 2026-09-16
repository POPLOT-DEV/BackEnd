package com.poplot.feature.auth.service

import com.poplot.feature.auth.repository.AuthSessionRepository
import com.poplot.feature.user.entity.User
import com.poplot.feature.user.repository.UserRepository
import com.poplot.global.config.JpaAuditingConfig
import com.poplot.global.security.jwt.JwtProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@ActiveProfiles("test")
@Import(
    JpaAuditingConfig::class,
    JwtProvider::class,
    RefreshTokenHasher::class,
    AuthTokenService::class,
)
class AuthTokenServiceTest @Autowired constructor(
    private val authTokenService: AuthTokenService,
    private val jwtProvider: JwtProvider,
    private val refreshTokenHasher: RefreshTokenHasher,
    private val userRepository: UserRepository,
    private val authSessionRepository: AuthSessionRepository,
) {
    @Test
    fun `issuing login tokens persists only the refresh token hash`() {
        val user = userRepository.save(User(email = "user@example.com"))

        val issued = authTokenService.issue(user)
        val session = authSessionRepository.findAll().single()

        assertThat(jwtProvider.isAccessToken(issued.accessToken)).isTrue()
        assertThat(jwtProvider.isRefreshToken(issued.refreshToken)).isTrue()
        assertThat(jwtProvider.getUserId(issued.accessToken)).isEqualTo(user.id)
        assertThat(issued.accessTokenExpiresIn).isEqualTo(3_600L)
        assertThat(session.refreshTokenHash).isEqualTo(refreshTokenHasher.hash(issued.refreshToken))
        assertThat(session.refreshTokenHash).doesNotContain(issued.refreshToken)
        assertThat(session.refreshTokenHash).hasSize(64)
        assertThat(session.expiresAt).isEqualTo(jwtProvider.getExpiration(issued.refreshToken))
        assertThat(session.revokedAt).isNull()
    }

    @Test
    fun `each login creates a distinct refresh session`() {
        val user = userRepository.save(User())

        val first = authTokenService.issue(user)
        val second = authTokenService.issue(user)

        assertThat(second.refreshToken).isNotEqualTo(first.refreshToken)
        assertThat(authSessionRepository.count()).isEqualTo(2)
    }
}
