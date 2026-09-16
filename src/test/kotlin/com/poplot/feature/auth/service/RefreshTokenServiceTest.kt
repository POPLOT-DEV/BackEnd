package com.poplot.feature.auth.service

import com.poplot.feature.auth.exception.AuthErrorCode
import com.poplot.feature.auth.repository.AuthSessionRepository
import com.poplot.feature.user.entity.User
import com.poplot.feature.user.enums.UserStatus
import com.poplot.feature.user.repository.UserRepository
import com.poplot.global.config.JpaAuditingConfig
import com.poplot.global.exception.GeneralException
import com.poplot.global.security.jwt.JwtProvider
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
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
    RefreshTokenService::class,
)
class RefreshTokenServiceTest @Autowired constructor(
    private val refreshTokenService: RefreshTokenService,
    private val authTokenService: AuthTokenService,
    private val jwtProvider: JwtProvider,
    private val userRepository: UserRepository,
    private val authSessionRepository: AuthSessionRepository,
) {
    @Test
    fun `Refresh Token을 교체하고 기존 세션을 폐기한다`() {
        val user = userRepository.save(User())
        val original = authTokenService.issue(user)

        val refreshed = refreshTokenService.refresh(original.refreshToken)

        val sessions = authSessionRepository.findAll().sortedBy { it.id }
        assertThat(refreshed.refreshToken).isNotEqualTo(original.refreshToken)
        assertThat(jwtProvider.isAccessToken(refreshed.accessToken)).isTrue()
        assertThat(jwtProvider.isRefreshToken(refreshed.refreshToken)).isTrue()
        assertThat(refreshed.accessTokenExpiresIn).isEqualTo(3_600L)
        assertThat(sessions).hasSize(2)
        assertThat(sessions.first().revokedAt).isNotNull()
        assertThat(sessions.first().lastUsedAt).isEqualTo(sessions.first().revokedAt)
        assertThat(sessions.last().revokedAt).isNull()
    }

    @Test
    fun `이미 사용한 Refresh Token은 다시 사용할 수 없다`() {
        val original = authTokenService.issue(userRepository.save(User()))
        refreshTokenService.refresh(original.refreshToken)

        assertAuthError(AuthErrorCode.UNUSABLE_REFRESH_TOKEN) {
            refreshTokenService.refresh(original.refreshToken)
        }
    }

    @Test
    fun `서버에 등록되지 않은 Refresh Token은 사용할 수 없다`() {
        val unregisteredToken = jwtProvider.createRefreshToken(999L)

        assertAuthError(AuthErrorCode.UNUSABLE_REFRESH_TOKEN) {
            refreshTokenService.refresh(unregisteredToken)
        }
    }

    @Test
    fun `Access Token과 손상된 토큰은 유효하지 않은 Refresh Token으로 거부한다`() {
        val user = userRepository.save(User())

        assertAuthError(AuthErrorCode.INVALID_REFRESH_TOKEN) {
            refreshTokenService.refresh(jwtProvider.createAccessToken(checkNotNull(user.id)))
        }
        assertAuthError(AuthErrorCode.INVALID_REFRESH_TOKEN) {
            refreshTokenService.refresh("invalid-token")
        }
    }

    @Test
    fun `만료된 Refresh Token은 만료 오류로 거부한다`() {
        val shortLivedJwtProvider = JwtProvider(
            secret = TEST_JWT_SECRET,
            accessTokenExpiration = 3_600_000,
            refreshTokenExpiration = 1,
        )
        val expiredToken = shortLivedJwtProvider.createRefreshToken(1L)
        Thread.sleep(10)

        assertAuthError(AuthErrorCode.EXPIRED_REFRESH_TOKEN) {
            refreshTokenService.refresh(expiredToken)
        }
    }

    @Test
    fun `비활성 사용자의 Refresh Token은 사용할 수 없다`() {
        val user = userRepository.save(User(status = UserStatus.INACTIVE))
        val issued = authTokenService.issue(user)

        assertAuthError(AuthErrorCode.UNUSABLE_REFRESH_TOKEN) {
            refreshTokenService.refresh(issued.refreshToken)
        }
    }

    @Test
    fun `Refresh Token이 없으면 필수 오류를 반환한다`() {
        assertAuthError(AuthErrorCode.REFRESH_TOKEN_REQUIRED) {
            refreshTokenService.refresh(" ")
        }
    }

    private fun assertAuthError(
        expected: AuthErrorCode,
        action: () -> Unit,
    ) {
        assertThatThrownBy(action)
            .isInstanceOfSatisfying(GeneralException::class.java) {
                assertThat(it.errorCode).isEqualTo(expected)
            }
    }

    private companion object {
        const val TEST_JWT_SECRET = "test-jwt-secret-key-for-poplot-backend-1234567890"
    }
}
