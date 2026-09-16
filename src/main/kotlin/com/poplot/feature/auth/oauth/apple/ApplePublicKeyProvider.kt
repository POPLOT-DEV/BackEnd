package com.poplot.feature.auth.oauth.apple

import com.fasterxml.jackson.annotation.JsonProperty
import com.poplot.feature.auth.exception.AuthErrorCode
import com.poplot.global.exception.GeneralException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.math.BigInteger
import java.security.KeyFactory
import java.security.PublicKey
import java.security.GeneralSecurityException
import java.security.spec.RSAPublicKeySpec
import java.time.Duration
import java.time.Instant
import java.util.Base64

fun interface ApplePublicKeyProvider {
    fun getPublicKey(keyId: String): PublicKey
}

@Component
@ConditionalOnProperty(prefix = "oauth.apple", name = ["enabled"], havingValue = "true")
class JwksApplePublicKeyProvider(
    @Qualifier("appleRestClient") private val restClient: RestClient,
    @Value("\${oauth.apple.public-key-cache-ttl}") private val cacheTtl: Duration,
) : ApplePublicKeyProvider {
    @Volatile
    private var cache = PublicKeyCache(emptyMap(), Instant.EPOCH)

    init {
        require(!cacheTtl.isNegative && !cacheTtl.isZero) {
            "Apple public key cache TTL must be positive."
        }
    }

    override fun getPublicKey(keyId: String): PublicKey {
        val snapshot = cache
        if (Instant.now().isBefore(snapshot.expiresAt)) {
            snapshot.keys[keyId]?.let { return it }
            if (snapshot.unknownKeyRefreshAttempted) {
                throw GeneralException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED)
            }
        }

        return synchronized(this) {
            val current = cache
            val currentIsValid = Instant.now().isBefore(current.expiresAt)
            if (currentIsValid) {
                current.keys[keyId]?.let { return@synchronized it }
                if (current.unknownKeyRefreshAttempted) {
                    throw GeneralException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED)
                }
                cache = current.copy(unknownKeyRefreshAttempted = true)
            }

            val refreshed = fetchPublicKeys()
            val publicKey = refreshed[keyId]
            cache = PublicKeyCache(
                keys = refreshed,
                expiresAt = Instant.now().plus(cacheTtl),
                unknownKeyRefreshAttempted = currentIsValid || publicKey == null,
            )
            publicKey ?: throw GeneralException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED)
        }
    }

    private fun fetchPublicKeys(): Map<String, PublicKey> =
        try {
            restClient.get()
                .uri(PUBLIC_KEYS_PATH)
                .retrieve()
                .body(AppleJwkSet::class.java)
                ?.keys
                ?.filter { it.keyType == RSA_KEY_TYPE && it.algorithm == RSA_ALGORITHM && it.use == SIGNATURE_USE }
                ?.associate { it.keyId to it.toPublicKey() }
                ?.takeIf { it.isNotEmpty() }
                ?: throw GeneralException(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE)
        } catch (exception: GeneralException) {
            throw exception
        } catch (exception: RestClientException) {
            throw GeneralException(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE)
        } catch (exception: IllegalArgumentException) {
            throw GeneralException(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE)
        } catch (exception: GeneralSecurityException) {
            throw GeneralException(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE)
        }

    private data class AppleJwkSet(
        val keys: List<AppleJwk> = emptyList(),
    )

    private data class AppleJwk(
        @JsonProperty("kty") val keyType: String,
        @JsonProperty("kid") val keyId: String,
        @JsonProperty("use") val use: String,
        @JsonProperty("alg") val algorithm: String,
        @JsonProperty("n") val modulus: String,
        @JsonProperty("e") val exponent: String,
    ) {
        fun toPublicKey(): PublicKey {
            val decoder = Base64.getUrlDecoder()
            val specification = RSAPublicKeySpec(
                BigInteger(1, decoder.decode(modulus)),
                BigInteger(1, decoder.decode(exponent)),
            )
            return KeyFactory.getInstance(RSA_KEY_TYPE).generatePublic(specification)
        }
    }

    private data class PublicKeyCache(
        val keys: Map<String, PublicKey>,
        val expiresAt: Instant,
        val unknownKeyRefreshAttempted: Boolean = false,
    )

    private companion object {
        const val PUBLIC_KEYS_PATH = "/auth/keys"
        const val RSA_KEY_TYPE = "RSA"
        const val RSA_ALGORITHM = "RS256"
        const val SIGNATURE_USE = "sig"
    }
}
