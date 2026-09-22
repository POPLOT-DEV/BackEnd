package com.poplot.feature.auth.dto.response;

import com.poplot.feature.auth.service.OAuthLoginResult;

public record OAuthLoginResponse(String accessToken, String refreshToken, UserResponse user) {
    public static OAuthLoginResponse from(OAuthLoginResult result) {
        return new OAuthLoginResponse(result.tokens().accessToken(), result.tokens().refreshToken(), UserResponse.from(result.user()));
    }
}
