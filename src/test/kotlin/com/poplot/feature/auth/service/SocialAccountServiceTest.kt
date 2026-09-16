package com.poplot.feature.auth.service

import com.poplot.feature.auth.entity.SocialAccount
import com.poplot.feature.auth.enums.SocialProvider
import com.poplot.feature.auth.exception.AuthErrorCode
import com.poplot.feature.auth.oauth.OAuthUserInfo
import com.poplot.feature.auth.repository.SocialAccountRepository
import com.poplot.feature.user.entity.User
import com.poplot.feature.user.enums.UserStatus
import com.poplot.feature.user.repository.UserRepository
import com.poplot.global.config.JpaAuditingConfig
import com.poplot.global.exception.GeneralException
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
    SocialAccountService::class,
    SocialAccountRegistrationService::class,
)
class SocialAccountServiceTest @Autowired constructor(
    private val socialAccountService: SocialAccountService,
    private val userRepository: UserRepository,
    private val socialAccountRepository: SocialAccountRepository,
) {
    @Test
    fun `first social login creates one user and social account`() {
        val userInfo = OAuthUserInfo("kakao-user-id", "user@example.com", "은우")

        val user = socialAccountService.findOrCreateUser(SocialProvider.KAKAO, userInfo)

        assertThat(user.id).isNotNull()
        assertThat(user.email).isEqualTo("user@example.com")
        assertThat(user.name).isEqualTo("은우")
        assertThat(userRepository.count()).isEqualTo(1)
        assertThat(socialAccountRepository.count()).isEqualTo(1)
    }

    @Test
    fun `repeated social login returns existing user without duplicates`() {
        val first = socialAccountService.findOrCreateUser(
            SocialProvider.APPLE,
            OAuthUserInfo("apple-user-id", "first@example.com", null),
        )

        val second = socialAccountService.findOrCreateUser(
            SocialProvider.APPLE,
            OAuthUserInfo("apple-user-id", "changed@example.com", "changed"),
        )

        assertThat(second.id).isEqualTo(first.id)
        assertThat(second.email).isEqualTo("first@example.com")
        assertThat(userRepository.count()).isEqualTo(1)
        assertThat(socialAccountRepository.count()).isEqualTo(1)
    }

    @Test
    fun `inactive social user is rejected`() {
        val user = userRepository.save(User(status = UserStatus.INACTIVE))
        socialAccountRepository.saveAndFlush(
            SocialAccount(user, SocialProvider.KAKAO, "inactive-user-id"),
        )

        assertAuthError(AuthErrorCode.INACTIVE_USER) {
            socialAccountService.findOrCreateUser(
                SocialProvider.KAKAO,
                OAuthUserInfo("inactive-user-id", null, null),
            )
        }
    }

    @Test
    fun `provider user id exceeding database limit is rejected before persistence`() {
        val userInfo = OAuthUserInfo("a".repeat(256), null, null)

        assertAuthError(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED) {
            socialAccountService.findOrCreateUser(SocialProvider.KAKAO, userInfo)
        }
        assertThat(userRepository.count()).isZero()
        assertThat(socialAccountRepository.count()).isZero()
    }

    private fun assertAuthError(expected: AuthErrorCode, action: () -> Unit) {
        assertThatThrownBy(action)
            .isInstanceOf(GeneralException::class.java)
            .extracting("errorCode")
            .isEqualTo(expected)
    }
}
