package com.poplot.feature.auth.oauth.apple;

import com.poplot.feature.auth.exception.AuthErrorCode;
import com.poplot.global.exception.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class JwksApplePublicKeyProviderTest {
    private static final String BASE_URL = "https://appleid.apple.test";
    private static final String KEY_ID = "apple-key-id";
    private MockRestServiceServer server;
    private JwksApplePublicKeyProvider provider;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        provider = new JwksApplePublicKeyProvider(builder.build(), Duration.ofHours(1));
    }

    @Test
    @DisplayName("matching apple public key is decoded and cached")
    void matchingKeyIsDecodedAndCached() throws Exception {
        RSAPublicKey expected = generatePublicKey();
        expectKeys(expected, KEY_ID);

        RSAPublicKey first = (RSAPublicKey) provider.getPublicKey(KEY_ID);
        PublicKey second = provider.getPublicKey(KEY_ID);

        assertThat(first.getModulus()).isEqualTo(expected.getModulus());
        assertThat(first.getPublicExponent()).isEqualTo(expected.getPublicExponent());
        assertThat(second).isSameAs(first);
        server.verify();
    }

    @Test
    @DisplayName("unknown key id is rejected after refreshing keys")
    void unknownKeyIsRejectedAfterRefresh() throws Exception {
        expectKeys(generatePublicKey(), KEY_ID);

        assertAuthError(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED, () -> provider.getPublicKey("unknown-key-id"));
    }

    @Test
    @DisplayName("unknown key ids trigger at most one forced refresh per cache generation")
    void unknownKeysCauseAtMostOneForcedRefresh() throws Exception {
        RSAPublicKey key = generatePublicKey();
        expectKeys(key, KEY_ID);
        expectKeys(key, KEY_ID);

        provider.getPublicKey(KEY_ID);

        assertAuthError(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED, () -> provider.getPublicKey("unknown-key-id-1"));
        assertAuthError(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED, () -> provider.getPublicKey("unknown-key-id-2"));
        server.verify();
    }

    @Test
    @DisplayName("unknown key id can resolve an apple signing key after one forced refresh")
    void rotatedKeyCanResolveAfterForcedRefresh() throws Exception {
        RSAPublicKey oldKey = generatePublicKey();
        RSAPublicKey rotated = generatePublicKey();
        expectKeys(oldKey, KEY_ID);
        expectKeys(rotated, "rotated-apple-key-id");

        provider.getPublicKey(KEY_ID);
        RSAPublicKey resolved = (RSAPublicKey) provider.getPublicKey("rotated-apple-key-id");

        assertThat(resolved.getModulus()).isEqualTo(rotated.getModulus());
        server.verify();
    }

    @Test
    @DisplayName("failed forced refresh is not repeated for other unknown key ids")
    void failedForcedRefreshIsNotRepeated() throws Exception {
        expectKeys(generatePublicKey(), KEY_ID);
        server.expect(requestTo(BASE_URL + "/auth/keys")).andRespond(withServerError());

        provider.getPublicKey(KEY_ID);

        assertAuthError(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE, () -> provider.getPublicKey("unknown-key-id-1"));
        assertAuthError(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED, () -> provider.getPublicKey("unknown-key-id-2"));
        server.verify();
    }

    @Test
    @DisplayName("apple public key server failure is mapped to unavailable error")
    void serverFailureMapsToUnavailable() {
        server.expect(requestTo(BASE_URL + "/auth/keys")).andRespond(withServerError());

        assertAuthError(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE, () -> provider.getPublicKey(KEY_ID));
    }

    @Test
    @DisplayName("malformed apple signing key is mapped to unavailable error")
    void malformedSigningKeyMapsToUnavailable() {
        server.expect(requestTo(BASE_URL + "/auth/keys"))
            .andRespond(withSuccess("{\"keys\":[{\"kty\":\"RSA\",\"kid\":\"apple-key-id\",\"use\":\"sig\",\"alg\":\"RS256\"}]}", MediaType.APPLICATION_JSON));

        assertAuthError(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE, () -> provider.getPublicKey(KEY_ID));
    }

    private void expectKeys(RSAPublicKey key, String keyId) {
        server.expect(requestTo(BASE_URL + "/auth/keys")).andRespond(withSuccess(jwkSet(key, keyId), MediaType.APPLICATION_JSON));
    }

    private String jwkSet(RSAPublicKey key, String keyId) {
        return "{\"keys\":[{\"kty\":\"RSA\",\"kid\":\"" + keyId + "\",\"use\":\"sig\",\"alg\":\"RS256\",\"n\":\"" + encode(key.getModulus().toByteArray()) + "\",\"e\":\"" + encode(key.getPublicExponent().toByteArray()) + "\"}]}";
    }

    private String encode(byte[] bytes) {
        byte[] unsigned = bytes[0] == 0 ? Arrays.copyOfRange(bytes, 1, bytes.length) : bytes;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(unsigned);
    }

    private RSAPublicKey generatePublicKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return (RSAPublicKey) generator.generateKeyPair().getPublic();
    }

    private void assertAuthError(AuthErrorCode expected, Runnable action) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(GeneralException.class, exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
