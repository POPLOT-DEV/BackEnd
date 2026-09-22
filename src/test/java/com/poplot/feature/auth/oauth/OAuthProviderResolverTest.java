package com.poplot.feature.auth.oauth;

import com.poplot.feature.auth.enums.SocialProvider;
import com.poplot.feature.auth.exception.AuthErrorCode;
import com.poplot.global.exception.GeneralException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuthProviderResolverTest {
    @Test
    @DisplayName("registered client is resolved by provider")
    void resolvesRegisteredClient() {
        OAuthProviderClient client = new StubClient(SocialProvider.KAKAO);
        assertThat(new OAuthProviderResolver(List.of(client)).resolve(SocialProvider.KAKAO)).isSameAs(client);
    }

    @Test
    @DisplayName("duplicate clients for one provider are rejected")
    void rejectsDuplicateClients() {
        assertThatThrownBy(() -> new OAuthProviderResolver(List.of(new StubClient(SocialProvider.KAKAO), new StubClient(SocialProvider.KAKAO))))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("missing client configuration is rejected")
    void rejectsMissingClientConfiguration() {
        OAuthProviderResolver resolver = new OAuthProviderResolver(List.of());
        assertThatThrownBy(() -> resolver.resolve(SocialProvider.APPLE))
            .isInstanceOfSatisfying(GeneralException.class, exception -> assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE));
    }

    private record StubClient(SocialProvider provider) implements OAuthProviderClient {
        @Override
        public SocialProvider getProvider() {
            return provider;
        }

        @Override
        public OAuthUserInfo authenticate(String token) {
            return new OAuthUserInfo("provider-user-id", null, null);
        }
    }
}
