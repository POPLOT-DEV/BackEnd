package com.poplot.feature.auth.service

import com.poplot.feature.auth.enums.SocialProvider
import com.poplot.feature.auth.exception.AuthErrorCode
import com.poplot.feature.auth.oauth.OAuthProviderResolver
import com.poplot.global.exception.GeneralException
import org.springframework.stereotype.Service

@Service
class OAuthLoginService(
    private val providerResolver: OAuthProviderResolver,
    private val socialAccountService: SocialAccountService,
    private val authTokenService: AuthTokenService,
) {
    fun login(
        providerValue: String,
        providerToken: String?,
    ): OAuthLoginResult {
        val provider = SocialProvider.from(providerValue)
        if (providerToken.isNullOrBlank()) {
            throw GeneralException(AuthErrorCode.OAUTH_TOKEN_REQUIRED)
        }

        val userInfo = providerResolver.resolve(provider).authenticate(providerToken)
        val user = socialAccountService.findOrCreateUser(provider, userInfo)
        return OAuthLoginResult(
            tokens = authTokenService.issue(user),
            user = user,
        )
    }
}
