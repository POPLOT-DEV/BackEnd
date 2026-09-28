package com.poplot.global.security.jwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtProviderTest {
    private final JwtProvider provider = new JwtProvider("test-jwt-secret-key-for-poplot-backend-1234567890", 3_600_000, 1_209_600_000);

    @Test
    @DisplayName("access and refresh tokens contain the expected type and user id")
    void accessAndRefreshTokensContainExpectedTypeAndUserId() {
        String access = provider.createAccessToken(1L);
        String refresh = provider.createRefreshToken(1L);

        assertThat(provider.getUserId(access)).isEqualTo(1L);
        assertThat(provider.isAccessToken(access)).isTrue();
        assertThat(provider.isRefreshToken(refresh)).isTrue();
        assertThat(provider.getAccessTokenExpirationSeconds()).isEqualTo(3_600L);
    }

    @Test
    @DisplayName("tokens issued to the same user are unique")
    void tokensIssuedToSameUserAreUnique() {
        String first = provider.createRefreshToken(1L);
        String second = provider.createRefreshToken(1L);

        assertThat(second).isNotEqualTo(first);
        assertThat(provider.parseClaims(first).getId()).isNotEqualTo(provider.parseClaims(second).getId());
    }

    @Test
    @DisplayName("role-aware access token contains the user's actual role")
    void roleAwareAccessTokenContainsActualRole() {
        String accessToken = provider.createAccessToken(1L, "ADMIN");

        assertThat(provider.parseClaims(accessToken).get("role", String.class)).isEqualTo("ADMIN");
    }
}
