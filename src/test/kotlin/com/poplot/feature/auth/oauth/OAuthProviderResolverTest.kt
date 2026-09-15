package com.poplot.feature.auth.oauth

import com.poplot.feature.auth.enums.SocialProvider
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class OAuthProviderResolverTest {
    @Test
    fun `registered client is resolved by provider`() {
        val kakaoClient = StubOAuthProviderClient(SocialProvider.KAKAO)
        val resolver = OAuthProviderResolver(listOf(kakaoClient))

        assertThat(resolver.resolve(SocialProvider.KAKAO)).isSameAs(kakaoClient)
    }

    @Test
    fun `duplicate clients for one provider are rejected`() {
        val firstClient = StubOAuthProviderClient(SocialProvider.KAKAO)
        val secondClient = StubOAuthProviderClient(SocialProvider.KAKAO)

        assertThatThrownBy { OAuthProviderResolver(listOf(firstClient, secondClient)) }
            .isInstanceOf(IllegalStateException::class.java)
    }

    @Test
    fun `missing client configuration is rejected`() {
        val resolver = OAuthProviderResolver(emptyList())

        assertThatThrownBy { resolver.resolve(SocialProvider.APPLE) }
            .isInstanceOf(IllegalStateException::class.java)
    }

    private class StubOAuthProviderClient(
        override val provider: SocialProvider,
    ) : OAuthProviderClient {
        override fun authenticate(providerToken: String): OAuthUserInfo =
            OAuthUserInfo(providerUserId = "provider-user-id", email = null, name = null)
    }
}
