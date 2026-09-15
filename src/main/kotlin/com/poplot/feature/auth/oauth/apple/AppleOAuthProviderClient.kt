package com.poplot.feature.auth.oauth.apple

import com.poplot.feature.auth.enums.SocialProvider
import com.poplot.feature.auth.exception.AuthErrorCode
import com.poplot.feature.auth.oauth.OAuthProviderClient
import com.poplot.feature.auth.oauth.OAuthUserInfo
import com.poplot.global.exception.GeneralException
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwsHeader
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.LocatorAdapter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.Key

@Component
class AppleOAuthProviderClient(
    private val publicKeyProvider: ApplePublicKeyProvider,
    @Value("\${oauth.apple.client-id}") private val clientId: String,
    @Value("\${oauth.apple.issuer}") private val issuer: String,
    @Value("\${oauth.apple.clock-skew-seconds}") private val clockSkewSeconds: Long,
) : OAuthProviderClient {
    override val provider = SocialProvider.APPLE

    init {
        require(clientId.isNotBlank()) { "Apple client ID must not be blank." }
        require(clockSkewSeconds >= 0) { "Apple clock skew must not be negative." }
    }

    override fun authenticate(providerToken: String): OAuthUserInfo {
        if (providerToken.isBlank()) {
            throw GeneralException(AuthErrorCode.OAUTH_TOKEN_REQUIRED)
        }

        val claims = try {
            Jwts.parser()
                .requireIssuer(issuer)
                .requireAudience(clientId)
                .clockSkewSeconds(clockSkewSeconds)
                .keyLocator(
                    object : LocatorAdapter<Key>() {
                        override fun locate(header: JwsHeader): Key {
                            if (header.algorithm != RSA_ALGORITHM || header.keyId.isNullOrBlank()) {
                                throw GeneralException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED)
                            }
                            return publicKeyProvider.getPublicKey(header.keyId)
                        }
                    },
                )
                .build()
                .parseSignedClaims(providerToken)
                .payload
        } catch (exception: ExpiredJwtException) {
            throw GeneralException(AuthErrorCode.OAUTH_TOKEN_EXPIRED)
        } catch (exception: GeneralException) {
            throw exception
        } catch (exception: JwtException) {
            throw GeneralException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED)
        } catch (exception: IllegalArgumentException) {
            throw GeneralException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED)
        }

        val subject = claims.subject?.takeIf(String::isNotBlank)
            ?: throw GeneralException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED)
        if (claims.expiration == null) {
            throw GeneralException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED)
        }
        val email = claims[EMAIL_CLAIM] as? String
        val emailVerified = when (val value = claims[EMAIL_VERIFIED_CLAIM]) {
            is Boolean -> value
            is String -> value.toBooleanStrictOrNull() == true
            else -> false
        }

        return OAuthUserInfo(
            providerUserId = subject,
            email = email?.takeIf { emailVerified && it.isNotBlank() },
            name = null,
        )
    }

    private companion object {
        const val RSA_ALGORITHM = "RS256"
        const val EMAIL_CLAIM = "email"
        const val EMAIL_VERIFIED_CLAIM = "email_verified"
    }
}
