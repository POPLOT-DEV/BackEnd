package com.poplot.feature.auth.oauth.apple;

import com.poplot.feature.auth.exception.AuthErrorCode;
import com.poplot.feature.auth.oauth.OAuthUserInfo;
import com.poplot.global.exception.GeneralException;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppleOAuthProviderClientTest {
    private static final String ISSUER = "https://appleid.apple.com";
    private static final String CLIENT_ID = "com.poplot.test";
    private static final String KEY_ID = "apple-key-id";
    private KeyPair keyPair;
    private AppleOAuthProviderClient client;

    @BeforeEach
    void setUp() throws Exception {
        keyPair = generateKeyPair();
        client = new AppleOAuthProviderClient(keyId -> {
            if (!KEY_ID.equals(keyId)) {
                throw new IllegalStateException("Unexpected key ID");
            }
            return keyPair.getPublic();
        }, CLIENT_ID, ISSUER, 0);
    }

    @Test
    @DisplayName("valid identity token returns apple user info")
    void validIdentityTokenReturnsUserInfo() {
        OAuthUserInfo info = client.authenticate(token(ISSUER, CLIENT_ID, Instant.now().plusSeconds(300), true, keyPair));
        assertThat(info.providerUserId()).isEqualTo("apple-user-id");
        assertThat(info.email()).isEqualTo("user@privaterelay.appleid.com");
        assertThat(info.name()).isNull();
    }

    @Test
    @DisplayName("unverified email is not used")
    void unverifiedEmailIsIgnored() {
        assertThat(client.authenticate(token(ISSUER, CLIENT_ID, Instant.now().plusSeconds(300), false, keyPair)).email()).isNull();
    }

    @Test
    @DisplayName("expired identity token is rejected")
    void expiredTokenIsRejected() {
        assertAuthError(AuthErrorCode.OAUTH_TOKEN_EXPIRED, () -> client.authenticate(token(ISSUER, CLIENT_ID, Instant.now().minusSeconds(1), true, keyPair)));
    }

    @Test
    @DisplayName("identity token with wrong issuer is rejected")
    void wrongIssuerIsRejected() {
        assertAuthError(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED, () -> client.authenticate(token("https://attacker.example", CLIENT_ID, Instant.now().plusSeconds(300), true, keyPair)));
    }

    @Test
    @DisplayName("identity token with wrong audience is rejected")
    void wrongAudienceIsRejected() {
        assertAuthError(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED, () -> client.authenticate(token(ISSUER, "com.attacker.app", Instant.now().plusSeconds(300), true, keyPair)));
    }

    @Test
    @DisplayName("identity token with invalid signature is rejected")
    void invalidSignatureIsRejected() throws Exception {
        assertAuthError(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED, () -> client.authenticate(token(ISSUER, CLIENT_ID, Instant.now().plusSeconds(300), true, generateUnchecked())));
    }

    @Test
    @DisplayName("identity token without expiration is rejected")
    void tokenWithoutExpirationIsRejected() {
        String token = Jwts.builder().header().keyId(KEY_ID).and().issuer(ISSUER).subject("apple-user-id")
            .audience().add(CLIENT_ID).and().signWith(keyPair.getPrivate()).compact();
        assertAuthError(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED, () -> client.authenticate(token));
    }

    @Test
    @DisplayName("blank identity token is rejected")
    void blankTokenIsRejected() {
        assertAuthError(AuthErrorCode.OAUTH_TOKEN_REQUIRED, () -> client.authenticate(" "));
    }

    private String token(String issuer, String audience, Instant expiration, boolean verified, KeyPair signingKey) {
        return Jwts.builder().header().keyId(KEY_ID).and().issuer(issuer).subject("apple-user-id").audience().add(audience).and()
            .expiration(Date.from(expiration)).claim("email", "user@privaterelay.appleid.com").claim("email_verified", verified)
            .signWith(signingKey.getPrivate()).compact();
    }

    private void assertAuthError(AuthErrorCode expected, Runnable action) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(GeneralException.class, exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }

    private KeyPair generateUnchecked() {
        try {
            return generateKeyPair();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private KeyPair generateKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }
}
