package com.poplot.global.security.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Component
class JwtProvider(
    @Value("\${jwt.secret}") secret: String,
    @Value("\${jwt.access-token-expiration}") private val accessTokenExpiration: Long,
    @Value("\${jwt.refresh-token-expiration}") private val refreshTokenExpiration: Long,
) {
    private val secretKey: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))

    init {
        require(accessTokenExpiration > 0) { "Access token expiration must be positive." }
        require(refreshTokenExpiration > 0) { "Refresh token expiration must be positive." }
    }

    fun createAccessToken(userId: Long): String =
        createToken(userId, ACCESS_TOKEN_TYPE, accessTokenExpiration)

    fun createRefreshToken(userId: Long): String =
        createToken(userId, REFRESH_TOKEN_TYPE, refreshTokenExpiration)

    fun parseClaims(token: String): Claims =
        Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).payload

    fun getUserId(token: String): Long = parseClaims(token).subject.toLong()

    fun isAccessToken(token: String): Boolean =
        parseClaims(token)[TOKEN_TYPE_CLAIM, String::class.java] == ACCESS_TOKEN_TYPE

    fun isRefreshToken(token: String): Boolean =
        parseClaims(token)[TOKEN_TYPE_CLAIM, String::class.java] == REFRESH_TOKEN_TYPE

    fun getExpiration(token: String): Instant = parseClaims(token).expiration.toInstant()

    fun getAccessTokenExpirationSeconds(): Long = accessTokenExpiration / MILLIS_PER_SECOND

    fun validateToken(token: String): Boolean =
        try {
            parseClaims(token)
            true
        } catch (_: JwtException) {
            false
        } catch (_: IllegalArgumentException) {
            false
        }

    private fun createToken(userId: Long, tokenType: String, expiration: Long): String {
        val now = Instant.now()
        return Jwts.builder()
            .id(UUID.randomUUID().toString())
            .subject(userId.toString())
            .claim(TOKEN_TYPE_CLAIM, tokenType)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusMillis(expiration)))
            .signWith(secretKey)
            .compact()
    }

    private companion object {
        const val TOKEN_TYPE_CLAIM = "type"
        const val ACCESS_TOKEN_TYPE = "access"
        const val REFRESH_TOKEN_TYPE = "refresh"
        const val MILLIS_PER_SECOND = 1_000L
    }
}
