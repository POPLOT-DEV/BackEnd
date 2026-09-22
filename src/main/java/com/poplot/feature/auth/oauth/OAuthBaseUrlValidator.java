package com.poplot.feature.auth.oauth;

import java.net.URI;

public final class OAuthBaseUrlValidator {
    private OAuthBaseUrlValidator() {
    }

    public static String requireHttpsBaseUrl(String baseUrl) {
        final URI uri;
        try {
            uri = URI.create(baseUrl);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("OAuth base URL must be a valid HTTPS URL.", exception);
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("OAuth base URL must be a valid HTTPS URL.");
        }
        return baseUrl;
    }
}
