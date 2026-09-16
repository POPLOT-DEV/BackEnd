package com.poplot.feature.auth.oauth

import com.poplot.feature.auth.enums.SocialProvider
import com.poplot.feature.auth.exception.AuthErrorCode
import com.poplot.global.exception.GeneralException
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
        clientsByProvider[provider]
            ?: throw GeneralException(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE)
}
