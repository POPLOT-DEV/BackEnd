package com.poplot.feature.auth.oauth.apple

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class AppleOAuthDisabledContextTest {
    private val contextRunner = ApplicationContextRunner()
        .withUserConfiguration(
            AppleOAuthConfig::class.java,
            AppleOAuthProviderClient::class.java,
            JwksApplePublicKeyProvider::class.java,
        )
        .withPropertyValues("oauth.apple.enabled=false")

    @Test
    fun `apple oauth beans are not created when integration is disabled`() {
        contextRunner.run { context ->
            assertThat(context).doesNotHaveBean(AppleOAuthProviderClient::class.java)
            assertThat(context).doesNotHaveBean(JwksApplePublicKeyProvider::class.java)
            assertThat(context).doesNotHaveBean("appleRestClient")
        }
    }
}
