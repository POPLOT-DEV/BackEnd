package com.poplot.feature.auth.service

data class IssuedAuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresIn: Long,
)
