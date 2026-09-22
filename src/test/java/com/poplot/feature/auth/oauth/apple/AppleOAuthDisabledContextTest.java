package com.poplot.feature.auth.oauth.apple;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class AppleOAuthDisabledContextTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(AppleOAuthConfig.class, AppleOAuthProviderClient.class, JwksApplePublicKeyProvider.class)
        .withPropertyValues("oauth.apple.enabled=false");

    @Test
    @DisplayName("apple oauth beans are not created when integration is disabled")
    void disabledIntegrationDoesNotCreateBeans() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(AppleOAuthProviderClient.class);
            assertThat(context).doesNotHaveBean(JwksApplePublicKeyProvider.class);
            assertThat(context).doesNotHaveBean("appleRestClient");
        });
    }
}
