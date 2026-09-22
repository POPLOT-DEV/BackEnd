package com.poplot.feature.auth.oauth;

import com.poplot.feature.auth.enums.SocialProvider;

public interface OAuthProviderClient {
    SocialProvider getProvider();
    OAuthUserInfo authenticate(String providerToken);
}
