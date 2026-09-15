package com.poplot.feature.auth.oauth

import com.poplot.feature.auth.enums.SocialProvider
import org.springframework.stereotype.Component

@Component
class OAuthProviderResolver(
    clients: List<OAuthProviderClient>,
) {
    private val clientsByProvider = clients.associateBy(OAuthProviderClient::provider)

    init {
        check(clientsByProvider.size == clients.size) {
            "Multiple OAuth clients are registered for the same provider."
        }
    }

    fun resolve(provider: SocialProvider): OAuthProviderClient =
        checkNotNull(clientsByProvider[provider]) {
            "OAuth client is not configured for provider: $provider"
        }
}
