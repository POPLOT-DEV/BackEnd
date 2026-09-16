package com.poplot.feature.auth.oauth.apple

import com.poplot.feature.auth.exception.AuthErrorCode
import com.poplot.global.exception.GeneralException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import java.time.Duration
import java.util.Base64

class JwksApplePublicKeyProviderTest {
    private lateinit var server: MockRestServiceServer
    private lateinit var provider: JwksApplePublicKeyProvider

    @BeforeEach
    fun setUp() {
        val builder = RestClient.builder().baseUrl(BASE_URL)
        server = MockRestServiceServer.bindTo(builder).build()
        provider = JwksApplePublicKeyProvider(builder.build(), Duration.ofHours(1))
    }

    @Test
    fun `matching apple public key is decoded and cached`() {
        val expected = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }
            .generateKeyPair().public as RSAPublicKey
        server.expect(requestTo("$BASE_URL/auth/keys"))
            .andRespond(withSuccess(jwkSet(expected), MediaType.APPLICATION_JSON))

        val first = provider.getPublicKey(KEY_ID) as RSAPublicKey
        val second = provider.getPublicKey(KEY_ID)

        assertThat(first.modulus).isEqualTo(expected.modulus)
        assertThat(first.publicExponent).isEqualTo(expected.publicExponent)
        assertThat(second).isSameAs(first)
        server.verify()
    }

    @Test
    fun `unknown key id is rejected after refreshing keys`() {
        val key = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }
            .generateKeyPair().public as RSAPublicKey
        server.expect(requestTo("$BASE_URL/auth/keys"))
            .andRespond(withSuccess(jwkSet(key), MediaType.APPLICATION_JSON))

        assertAuthError(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED) {
            provider.getPublicKey("unknown-key-id")
        }
    }

    @Test
    fun `unknown key ids trigger at most one forced refresh per cache generation`() {
        val key = generatePublicKey()
        server.expect(requestTo("$BASE_URL/auth/keys"))
            .andRespond(withSuccess(jwkSet(key), MediaType.APPLICATION_JSON))
        server.expect(requestTo("$BASE_URL/auth/keys"))
            .andRespond(withSuccess(jwkSet(key), MediaType.APPLICATION_JSON))
        provider.getPublicKey(KEY_ID)

        assertAuthError(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED) {
            provider.getPublicKey("unknown-key-id-1")
        }
        assertAuthError(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED) {
            provider.getPublicKey("unknown-key-id-2")
        }
        server.verify()
    }

    @Test
    fun `unknown key id can resolve an apple signing key after one forced refresh`() {
        val oldKey = generatePublicKey()
        val rotatedKey = generatePublicKey()
        server.expect(requestTo("$BASE_URL/auth/keys"))
            .andRespond(withSuccess(jwkSet(oldKey), MediaType.APPLICATION_JSON))
        server.expect(requestTo("$BASE_URL/auth/keys"))
            .andRespond(withSuccess(jwkSet(rotatedKey, ROTATED_KEY_ID), MediaType.APPLICATION_JSON))
        provider.getPublicKey(KEY_ID)

        val resolved = provider.getPublicKey(ROTATED_KEY_ID) as RSAPublicKey

        assertThat(resolved.modulus).isEqualTo(rotatedKey.modulus)
        server.verify()
    }

    @Test
    fun `failed forced refresh is not repeated for other unknown key ids`() {
        val key = generatePublicKey()
        server.expect(requestTo("$BASE_URL/auth/keys"))
            .andRespond(withSuccess(jwkSet(key), MediaType.APPLICATION_JSON))
        server.expect(requestTo("$BASE_URL/auth/keys"))
            .andRespond(withServerError())
        provider.getPublicKey(KEY_ID)

        assertAuthError(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE) {
            provider.getPublicKey("unknown-key-id-1")
        }
        assertAuthError(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED) {
            provider.getPublicKey("unknown-key-id-2")
        }
        server.verify()
    }

    @Test
    fun `apple public key server failure is mapped to unavailable error`() {
        server.expect(requestTo("$BASE_URL/auth/keys"))
            .andRespond(withServerError())

        assertAuthError(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE) {
            provider.getPublicKey(KEY_ID)
        }
    }

    private fun jwkSet(
        key: RSAPublicKey,
        keyId: String = KEY_ID,
    ): String =
        """{"keys":[{"kty":"RSA","kid":"$keyId","use":"sig","alg":"RS256","n":"${encode(key.modulus.toByteArray())}","e":"${encode(key.publicExponent.toByteArray())}"}]}"""

    private fun generatePublicKey(): RSAPublicKey =
        KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }
            .generateKeyPair().public as RSAPublicKey

    private fun encode(bytes: ByteArray): String {
        val unsigned = if (bytes.first() == 0.toByte()) bytes.copyOfRange(1, bytes.size) else bytes
        return Base64.getUrlEncoder().withoutPadding().encodeToString(unsigned)
    }

    private fun assertAuthError(expected: AuthErrorCode, action: () -> Unit) {
        assertThatThrownBy(action)
            .isInstanceOf(GeneralException::class.java)
            .extracting("errorCode")
            .isEqualTo(expected)
    }

    private companion object {
        const val BASE_URL = "https://appleid.apple.test"
        const val KEY_ID = "apple-key-id"
        const val ROTATED_KEY_ID = "rotated-apple-key-id"
    }
}
