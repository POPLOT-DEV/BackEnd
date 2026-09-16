package com.poplot.feature.auth.oauth

data class OAuthUserInfo(
    val providerUserId: String,
    val email: String?,
    val name: String?,
) {
    init {
        require(providerUserId.isNotBlank()) {
            "OAuth provider user ID must not be blank."
        }
    }
}
