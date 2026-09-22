package com.poplot.feature.auth.service;

import com.poplot.feature.user.entity.User;

public record OAuthLoginResult(IssuedAuthTokens tokens, User user) {
}
