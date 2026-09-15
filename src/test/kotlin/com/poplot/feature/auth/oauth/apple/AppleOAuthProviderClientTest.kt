package com.poplot.feature.auth.oauth.apple

import com.poplot.feature.auth.exception.AuthErrorCode
import com.poplot.global.exception.GeneralException
import io.jsonwebtoken.Jwts
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.time.Instant
import java.util.Date

class AppleOAuthProviderClientTest {
    private lateinit var keyPair: KeyPair
    private lateinit var client: AppleOAuthProviderClient

    @BeforeEach
    fun setUp() {
        keyPair = generateKeyPair()
        client = AppleOAuthProviderClient(
            publicKeyProvider = ApplePublicKeyProvider { keyId ->
                check(keyId == KEY_ID)
                keyPair.public
            },
            clientId = CLIENT_ID,
            issuer = ISSUER,
            clockSkewSeconds = 0,
        )
    }

    @Test
    fun `valid identity token returns apple user info`() {
        val token = createToken(emailVerified = true)

        val result = client.authenticate(token)

        assertThat(result.providerUserId).isEqualTo(SUBJECT)
        assertThat(result.email).isEqualTo("user@privaterelay.appleid.com")
        assertThat(result.name).isNull()
    }

    @Test
    fun `unverified email is not used`() {
        val token = createToken(emailVerified = false)

        assertThat(client.authenticate(token).email).isNull()
    }

    @Test
    fun `expired identity token is rejected`() {
        val token = createToken(expiration = Instant.now().minusSeconds(1))

        assertAuthError(AuthErrorCode.OAUTH_TOKEN_EXPIRED) {
            client.authenticate(token)
        }
    }

    @Test
    fun `identity token with wrong issuer is rejected`() {
        val token = createToken(issuer = "https://attacker.example")

        assertAuthError(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED) {
            client.authenticate(token)
        }
    }

    @Test
    fun `identity token with wrong audience is rejected`() {
        val token = createToken(audience = "com.attacker.app")

        assertAuthError(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED) {
            client.authenticate(token)
        }
    }

    @Test
    fun `identity token with invalid signature is rejected`() {
        val token = createToken(signingKeyPair = generateKeyPair())

        assertAuthError(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED) {
            client.authenticate(token)
        }
    }

    @Test
    fun `identity token without expiration is rejected`() {
        val token = Jwts.builder()
            .header().keyId(KEY_ID).and()
            .issuer(ISSUER)
            .subject(SUBJECT)
            .audience().add(CLIENT_ID).and()
            .signWith(keyPair.private)
            .compact()

        assertAuthError(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED) {
            client.authenticate(token)
        }
    }

    @Test
    fun `blank identity token is rejected`() {
        assertAuthError(AuthErrorCode.OAUTH_TOKEN_REQUIRED) {
            client.authenticate(" ")
        }
    }

    private fun createToken(
        issuer: String = ISSUER,
        audience: String = CLIENT_ID,
        expiration: Instant = Instant.now().plusSeconds(300),
        emailVerified: Boolean = true,
        signingKeyPair: KeyPair = keyPair,
    ): String =
        Jwts.builder()
            .header().keyId(KEY_ID).and()
            .issuer(issuer)
            .subject(SUBJECT)
            .audience().add(audience).and()
            .expiration(Date.from(expiration))
            .claim("email", "user@privaterelay.appleid.com")
            .claim("email_verified", emailVerified)
            .signWith(signingKeyPair.private)
            .compact()

    private fun assertAuthError(expected: AuthErrorCode, action: () -> Unit) {
        assertThatThrownBy(action)
            .isInstanceOf(GeneralException::class.java)
            .extracting("errorCode")
            .isEqualTo(expected)
    }

    private fun generateKeyPair(): KeyPair =
        KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

    private companion object {
        const val ISSUER = "https://appleid.apple.com"
        const val CLIENT_ID = "com.poplot.test"
        const val KEY_ID = "apple-key-id"
        const val SUBJECT = "apple-user-id"
    }
}
