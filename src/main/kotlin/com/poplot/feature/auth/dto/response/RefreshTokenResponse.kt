package com.poplot.feature.auth.dto.response

import com.poplot.feature.auth.service.IssuedAuthTokens

data class RefreshTokenResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
) {
    companion object {
        fun from(tokens: IssuedAuthTokens): RefreshTokenResponse =
            RefreshTokenResponse(
                accessToken = tokens.accessToken,
                refreshToken = tokens.refreshToken,
                expiresIn = tokens.accessTokenExpiresIn,
            )
    }
}
