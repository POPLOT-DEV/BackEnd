package com.poplot.feature.auth.oauth.apple;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.poplot.feature.auth.exception.AuthErrorCode;
import com.poplot.global.exception.GeneralException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "oauth.apple", name = "enabled", havingValue = "true")
public class JwksApplePublicKeyProvider implements ApplePublicKeyProvider {
    private final RestClient restClient;
    private final Duration cacheTtl;
    private volatile PublicKeyCache cache = new PublicKeyCache(Map.of(), Instant.EPOCH, false);

    public JwksApplePublicKeyProvider(@Qualifier("appleRestClient") RestClient restClient,
                                      @Value("${oauth.apple.public-key-cache-ttl}") Duration cacheTtl) {
        if (cacheTtl.isNegative() || cacheTtl.isZero()) {
            throw new IllegalArgumentException("Apple public key cache TTL must be positive.");
        }
        this.restClient = restClient;
        this.cacheTtl = cacheTtl;
    }

    @Override
    public PublicKey getPublicKey(String keyId) {
        PublicKeyCache snapshot = cache;
        if (Instant.now().isBefore(snapshot.expiresAt())) {
            PublicKey key = snapshot.keys().get(keyId);
            if (key != null) {
                return key;
            }
            if (snapshot.unknownKeyRefreshAttempted()) {
                throw new GeneralException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
            }
        }
        synchronized (this) {
            PublicKeyCache current = cache;
            boolean currentIsValid = Instant.now().isBefore(current.expiresAt());
            if (currentIsValid) {
                PublicKey key = current.keys().get(keyId);
                if (key != null) {
                    return key;
                }
                if (current.unknownKeyRefreshAttempted()) {
                    throw new GeneralException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
                }
                cache = new PublicKeyCache(current.keys(), current.expiresAt(), true);
            }
            Map<String, PublicKey> refreshed = fetchPublicKeys();
            PublicKey publicKey = refreshed.get(keyId);
            cache = new PublicKeyCache(refreshed, Instant.now().plus(cacheTtl), currentIsValid || publicKey == null);
            if (publicKey == null) {
                throw new GeneralException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
            }
            return publicKey;
        }
    }

    private Map<String, PublicKey> fetchPublicKeys() {
        try {
            AppleJwkSet set = restClient.get().uri("/auth/keys").retrieve().body(AppleJwkSet.class);
            if (set == null || set.keys() == null) {
                throw new GeneralException(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE);
            }
            Map<String, PublicKey> keys = new LinkedHashMap<>();
            for (AppleJwk key : set.keys()) {
                if ("RSA".equals(key.keyType()) && "RS256".equals(key.algorithm()) && "sig".equals(key.use())) {
                    keys.put(key.keyId(), key.toPublicKey());
                }
            }
            if (keys.isEmpty()) {
                throw new GeneralException(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE);
            }
            return keys;
        } catch (GeneralException exception) {
            throw exception;
        } catch (RestClientException | IllegalArgumentException | InvalidPublicKeyException exception) {
            throw new GeneralException(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE);
        }
    }

    private record AppleJwkSet(List<AppleJwk> keys) {
        private AppleJwkSet {
            if (keys == null) {
                keys = List.of();
            }
        }
    }

    private record AppleJwk(@JsonProperty("kty") String keyType, @JsonProperty("kid") String keyId,
                            @JsonProperty("use") String use, @JsonProperty("alg") String algorithm,
                            @JsonProperty("n") String modulus, @JsonProperty("e") String exponent) {
        PublicKey toPublicKey() {
            try {
                if (keyId == null || keyId.isBlank() || modulus == null || modulus.isBlank() || exponent == null || exponent.isBlank()) {
                    throw new InvalidPublicKeyException();
                }
                Base64.Decoder decoder = Base64.getUrlDecoder();
                return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(
                    new BigInteger(1, decoder.decode(modulus)), new BigInteger(1, decoder.decode(exponent))));
            } catch (GeneralSecurityException exception) {
                throw new InvalidPublicKeyException(exception);
            }
        }
    }

    private record PublicKeyCache(Map<String, PublicKey> keys, Instant expiresAt, boolean unknownKeyRefreshAttempted) {
    }

    private static class InvalidPublicKeyException extends RuntimeException {
        InvalidPublicKeyException() {
            super();
        }

        InvalidPublicKeyException(GeneralSecurityException cause) {
            super(cause);
        }
    }
}
