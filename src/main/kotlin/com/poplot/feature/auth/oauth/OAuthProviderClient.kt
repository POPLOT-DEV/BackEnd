package com.poplot.feature.auth.oauth

import com.poplot.feature.auth.enums.SocialProvider

interface OAuthProviderClient {
    val provider: SocialProvider

    fun authenticate(providerToken: String): OAuthUserInfo
}
