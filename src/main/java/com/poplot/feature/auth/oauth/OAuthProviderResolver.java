package com.poplot.feature.auth.oauth;

import com.poplot.feature.auth.enums.SocialProvider;
import com.poplot.feature.auth.exception.AuthErrorCode;
import com.poplot.global.exception.GeneralException;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class OAuthProviderResolver {
    private final Map<SocialProvider, OAuthProviderClient> clientsByProvider;

    public OAuthProviderResolver(List<OAuthProviderClient> clients) {
        clientsByProvider = new EnumMap<>(SocialProvider.class);
        for (OAuthProviderClient client : clients) {
            if (clientsByProvider.put(client.getProvider(), client) != null) {
                throw new IllegalStateException("Multiple OAuth clients are registered for the same provider.");
            }
        }
    }

    public OAuthProviderClient resolve(SocialProvider provider) {
        OAuthProviderClient client = clientsByProvider.get(provider);
        if (client == null) throw new GeneralException(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE);
        return client;
    }
}
