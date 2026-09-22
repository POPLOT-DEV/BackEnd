package com.poplot.feature.auth.service;

import com.poplot.feature.auth.enums.SocialProvider;
import com.poplot.feature.auth.exception.AuthErrorCode;
import com.poplot.feature.auth.oauth.OAuthProviderClient;
import com.poplot.feature.auth.oauth.OAuthProviderResolver;
import com.poplot.feature.auth.oauth.OAuthUserInfo;
import com.poplot.feature.user.entity.User;
import com.poplot.global.exception.GeneralException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OAuthLoginServiceTest {
    private final OAuthProviderClient providerClient = mock(OAuthProviderClient.class);
    private final OAuthProviderResolver providerResolver = mock(OAuthProviderResolver.class);
    private final SocialAccountService socialAccountService = mock(SocialAccountService.class);
    private final AuthTokenService authTokenService = mock(AuthTokenService.class);
    private final OAuthLoginService service = new OAuthLoginService(providerResolver, socialAccountService, authTokenService);

    @Test
    @DisplayName("검증된 소셜 사용자로 로그인하고 POPLOT 토큰을 발급한다")
    void issuesPoplotTokensForVerifiedSocialUser() {
        OAuthUserInfo userInfo = new OAuthUserInfo("provider-user-id", "user@example.com", "은우");
        User user = User.create(userInfo.email(), userInfo.name());
        IssuedAuthTokens tokens = new IssuedAuthTokens("access-token", "refresh-token", 3600);
        when(providerResolver.resolve(SocialProvider.KAKAO)).thenReturn(providerClient);
        when(providerClient.authenticate("provider-token")).thenReturn(userInfo);
        when(socialAccountService.findOrCreateUser(SocialProvider.KAKAO, userInfo)).thenReturn(user);
        when(authTokenService.issue(user)).thenReturn(tokens);

        OAuthLoginResult result = service.login("kakao", "provider-token");

        assertThat(result.user()).isSameAs(user);
        assertThat(result.tokens()).isEqualTo(tokens);
    }

    @Test
    @DisplayName("OAuth 토큰이 비어 있으면 Provider를 호출하지 않는다")
    void blankTokenDoesNotCallProvider() {
        assertThatThrownBy(() -> service.login("kakao", " "))
            .isInstanceOfSatisfying(GeneralException.class, exception -> assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.OAUTH_TOKEN_REQUIRED));

        verify(providerResolver, never()).resolve(SocialProvider.KAKAO);
    }

    @Test
    @DisplayName("지원하지 않는 Provider는 거부한다")
    void unsupportedProviderIsRejected() {
        assertThatThrownBy(() -> service.login("google", "provider-token"))
            .isInstanceOfSatisfying(GeneralException.class, exception -> assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.UNSUPPORTED_OAUTH_PROVIDER));
    }
}
