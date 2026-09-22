package com.poplot.feature.auth.oauth;

public record OAuthUserInfo(String providerUserId, String email, String name) {
    public OAuthUserInfo {
        if (providerUserId == null || providerUserId.isBlank()) {
            throw new IllegalArgumentException("OAuth provider user ID must not be blank.");
        }
    }
}
