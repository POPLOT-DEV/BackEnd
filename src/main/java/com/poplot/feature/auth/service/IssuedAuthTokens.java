package com.poplot.feature.auth.service;

public record IssuedAuthTokens(String accessToken, String refreshToken, long accessTokenExpiresIn) {
}
