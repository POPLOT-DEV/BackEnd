package com.poplot.feature.auth.service

import com.poplot.feature.auth.entity.SocialAccount
import com.poplot.feature.auth.enums.SocialProvider
import com.poplot.feature.auth.oauth.OAuthUserInfo
import com.poplot.feature.auth.repository.SocialAccountRepository
import com.poplot.feature.user.entity.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.dao.DataIntegrityViolationException

class SocialAccountConcurrencyRecoveryTest {
    @Test
    fun `unique conflict caused by concurrent first login returns committed account`() {
        val repository = mock(SocialAccountRepository::class.java)
        val registrationService = mock(SocialAccountRegistrationService::class.java)
        val userInfo = OAuthUserInfo("provider-user-id", null, null)
        val committedUser = User()
        val committedAccount = SocialAccount(
            committedUser,
            SocialProvider.KAKAO,
            userInfo.providerUserId,
        )
        `when`(
            repository.findByProviderAndProviderUserId(SocialProvider.KAKAO, userInfo.providerUserId),
        ).thenReturn(null, committedAccount)
        `when`(
            registrationService.createIfAbsent(SocialProvider.KAKAO, userInfo),
        ).thenThrow(DataIntegrityViolationException("duplicate social account"))
        val service = SocialAccountService(repository, registrationService)

        val result = service.findOrCreateUser(SocialProvider.KAKAO, userInfo)

        assertThat(result).isSameAs(committedUser)
    }
}
