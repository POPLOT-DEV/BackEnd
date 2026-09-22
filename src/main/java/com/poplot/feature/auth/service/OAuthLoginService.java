package com.poplot.feature.auth.service;

import com.poplot.feature.auth.enums.SocialProvider;
import com.poplot.feature.auth.exception.AuthErrorCode;
import com.poplot.feature.auth.oauth.OAuthProviderResolver;
import com.poplot.feature.user.entity.User;
import com.poplot.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OAuthLoginService {
    private final OAuthProviderResolver providerResolver;
    private final SocialAccountService socialAccountService;
    private final AuthTokenService authTokenService;

    public OAuthLoginResult login(String providerValue, String providerToken) {
        SocialProvider provider = SocialProvider.from(providerValue);
        if (providerToken == null || providerToken.isBlank()) {
            throw new GeneralException(AuthErrorCode.OAUTH_TOKEN_REQUIRED);
        }
        User user = socialAccountService.findOrCreateUser(provider, providerResolver.resolve(provider).authenticate(providerToken));
        return new OAuthLoginResult(authTokenService.issue(user), user);
    }
}
