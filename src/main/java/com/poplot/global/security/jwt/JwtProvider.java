package com.poplot.global.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtProvider {
    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtProvider(@Value("${jwt.secret}") String secret,
                       @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
                       @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        if (accessTokenExpiration <= 0) {
            throw new IllegalArgumentException("Access token expiration must be positive.");
        }
        if (refreshTokenExpiration <= 0) {
            throw new IllegalArgumentException("Refresh token expiration must be positive.");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String createAccessToken(long userId) {
        return createToken(userId, "access", accessTokenExpiration);
    }

    public String createAccessToken(long userId, String role) {
        Instant now = Instant.now();
        return Jwts.builder().id(UUID.randomUUID().toString()).subject(Long.toString(userId)).claim("type", "access")
            .claim("role", role).issuedAt(Date.from(now)).expiration(Date.from(now.plusMillis(accessTokenExpiration)))
            .signWith(secretKey).compact();
    }

    public String createRefreshToken(long userId) {
        return createToken(userId, "refresh", refreshTokenExpiration);
    }

    public Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    }

    public long getUserId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    public boolean isAccessToken(String token) {
        return "access".equals(parseClaims(token).get("type", String.class));
    }

    public boolean isRefreshToken(String token) {
        return "refresh".equals(parseClaims(token).get("type", String.class));
    }

    public Instant getExpiration(String token) {
        return parseClaims(token).getExpiration().toInstant();
    }

    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpiration / 1_000L;
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception) {
            return false;
        }
    }

    private String createToken(long userId, String tokenType, long expiration) {
        Instant now = Instant.now();
        return Jwts.builder().id(UUID.randomUUID().toString()).subject(Long.toString(userId)).claim("type", tokenType)
            .issuedAt(Date.from(now)).expiration(Date.from(now.plusMillis(expiration))).signWith(secretKey).compact();
    }
}
