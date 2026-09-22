package com.poplot.feature.auth.dto.response;

import com.poplot.feature.auth.service.IssuedAuthTokens;

public record RefreshTokenResponse(String accessToken, String refreshToken, long expiresIn) {
    public static RefreshTokenResponse from(IssuedAuthTokens tokens) {
        return new RefreshTokenResponse(tokens.accessToken(), tokens.refreshToken(), tokens.accessTokenExpiresIn());
    }
}
