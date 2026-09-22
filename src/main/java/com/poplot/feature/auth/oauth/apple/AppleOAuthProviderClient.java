package com.poplot.feature.auth.oauth.apple;

import com.poplot.feature.auth.enums.SocialProvider;
import com.poplot.feature.auth.exception.AuthErrorCode;
import com.poplot.feature.auth.oauth.OAuthProviderClient;
import com.poplot.feature.auth.oauth.OAuthUserInfo;
import com.poplot.global.exception.GeneralException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.LocatorAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.security.Key;

@Component
@ConditionalOnProperty(prefix = "oauth.apple", name = "enabled", havingValue = "true")
public class AppleOAuthProviderClient implements OAuthProviderClient {
    private final ApplePublicKeyProvider publicKeyProvider;
    private final String clientId;
    private final String issuer;
    private final long clockSkewSeconds;

    public AppleOAuthProviderClient(ApplePublicKeyProvider publicKeyProvider,
                                    @Value("${oauth.apple.client-id}") String clientId,
                                    @Value("${oauth.apple.issuer}") String issuer,
                                    @Value("${oauth.apple.clock-skew-seconds}") long clockSkewSeconds) {
        if (clientId.isBlank()) {
            throw new IllegalArgumentException("Apple client ID must not be blank.");
        }
        if (clockSkewSeconds < 0) {
            throw new IllegalArgumentException("Apple clock skew must not be negative.");
        }
        this.publicKeyProvider = publicKeyProvider;
        this.clientId = clientId;
        this.issuer = issuer;
        this.clockSkewSeconds = clockSkewSeconds;
    }

    @Override
    public SocialProvider getProvider() {
        return SocialProvider.APPLE;
    }

    @Override
    public OAuthUserInfo authenticate(String providerToken) {
        if (providerToken.isBlank()) {
            throw new GeneralException(AuthErrorCode.OAUTH_TOKEN_REQUIRED);
        }
        final Claims claims;
        try {
            claims = Jwts.parser().requireIssuer(issuer).requireAudience(clientId).clockSkewSeconds(clockSkewSeconds)
                .keyLocator(new LocatorAdapter<Key>() {
                    @Override
                    public Key locate(JwsHeader header) {
                        if (!"RS256".equals(header.getAlgorithm()) || header.getKeyId() == null || header.getKeyId().isBlank()) {
                            throw new GeneralException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
                        }
                        return publicKeyProvider.getPublicKey(header.getKeyId());
                    }
                }).build().parseSignedClaims(providerToken).getPayload();
        } catch (ExpiredJwtException exception) {
            throw new GeneralException(AuthErrorCode.OAUTH_TOKEN_EXPIRED);
        } catch (GeneralException exception) {
            throw exception;
        } catch (JwtException | IllegalArgumentException exception) {
            throw new GeneralException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
        }
        String subject = claims.getSubject();
        if (subject == null || subject.isBlank() || claims.getExpiration() == null) {
            throw new GeneralException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
        }
        Object verifiedClaim = claims.get("email_verified");
        boolean emailVerified = verifiedClaim instanceof Boolean booleanValue ? booleanValue : verifiedClaim instanceof String stringValue && "true".equals(stringValue);
        Object emailClaim = claims.get("email");
        String email = emailVerified && emailClaim instanceof String value && !value.isBlank() ? value : null;
        return new OAuthUserInfo(subject, email, null);
    }
}
