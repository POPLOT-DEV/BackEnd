package com.poplot.feature.auth.service

import com.poplot.feature.user.entity.User

data class OAuthLoginResult(
    val tokens: IssuedAuthTokens,
    val user: User,
)
