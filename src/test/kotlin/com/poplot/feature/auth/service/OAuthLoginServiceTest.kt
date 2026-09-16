package com.poplot.feature.auth.service

import com.poplot.feature.auth.enums.SocialProvider
import com.poplot.feature.auth.exception.AuthErrorCode
import com.poplot.feature.auth.oauth.OAuthProviderClient
import com.poplot.feature.auth.oauth.OAuthProviderResolver
import com.poplot.feature.auth.oauth.OAuthUserInfo
import com.poplot.feature.user.entity.User
import com.poplot.global.exception.GeneralException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class OAuthLoginServiceTest {
    private val providerClient = mock(OAuthProviderClient::class.java)
    private val providerResolver = mock(OAuthProviderResolver::class.java)
    private val socialAccountService = mock(SocialAccountService::class.java)
    private val authTokenService = mock(AuthTokenService::class.java)
    private val service = OAuthLoginService(providerResolver, socialAccountService, authTokenService)

    @Test
    fun `검증된 소셜 사용자로 로그인하고 POPLOT 토큰을 발급한다`() {
        val userInfo = OAuthUserInfo("provider-user-id", "user@example.com", "은우")
        val user = User(email = userInfo.email, name = userInfo.name, id = 1L)
        val tokens = IssuedAuthTokens("access-token", "refresh-token", 3600)
        `when`(providerResolver.resolve(SocialProvider.KAKAO)).thenReturn(providerClient)
        `when`(providerClient.authenticate("provider-token")).thenReturn(userInfo)
        `when`(socialAccountService.findOrCreateUser(SocialProvider.KAKAO, userInfo)).thenReturn(user)
        `when`(authTokenService.issue(user)).thenReturn(tokens)

        val result = service.login("kakao", "provider-token")

        assertThat(result.user).isSameAs(user)
        assertThat(result.tokens).isEqualTo(tokens)
    }

    @Test
    fun `OAuth 토큰이 비어 있으면 Provider를 호출하지 않는다`() {
        assertThatThrownBy { service.login("kakao", " ") }
            .isInstanceOfSatisfying(GeneralException::class.java) {
                assertThat(it.errorCode).isEqualTo(AuthErrorCode.OAUTH_TOKEN_REQUIRED)
            }

        verify(providerResolver, never()).resolve(SocialProvider.KAKAO)
    }

    @Test
    fun `지원하지 않는 Provider는 거부한다`() {
        assertThatThrownBy { service.login("google", "provider-token") }
            .isInstanceOfSatisfying(GeneralException::class.java) {
                assertThat(it.errorCode).isEqualTo(AuthErrorCode.UNSUPPORTED_OAUTH_PROVIDER)
            }
    }
}
