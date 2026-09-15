package com.poplot.feature.auth.oauth.kakao

import com.fasterxml.jackson.annotation.JsonProperty
import com.poplot.feature.auth.enums.SocialProvider
import com.poplot.feature.auth.exception.AuthErrorCode
import com.poplot.feature.auth.oauth.OAuthProviderClient
import com.poplot.feature.auth.oauth.OAuthUserInfo
import com.poplot.global.exception.GeneralException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException

@Component
class KakaoOAuthProviderClient(
    @Qualifier("kakaoRestClient") private val restClient: RestClient,
) : OAuthProviderClient {
    override val provider = SocialProvider.KAKAO

    override fun authenticate(providerToken: String): OAuthUserInfo {
        if (providerToken.isBlank()) {
            throw GeneralException(AuthErrorCode.OAUTH_TOKEN_REQUIRED)
        }

        val response = try {
            restClient.get()
                .uri(USER_INFO_PATH)
                .headers { it.setBearerAuth(providerToken) }
                .retrieve()
                .body(KakaoUserResponse::class.java)
                ?: throw GeneralException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED)
        } catch (exception: HttpStatusCodeException) {
            throw mapHttpException(exception)
        } catch (exception: ResourceAccessException) {
            throw GeneralException(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE)
        } catch (exception: RestClientException) {
            throw GeneralException(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE)
        }

        return response.toOAuthUserInfo()
    }

    private fun mapHttpException(exception: HttpStatusCodeException): GeneralException {
        val errorCode = when {
            exception.statusCode == HttpStatus.UNAUTHORIZED &&
                exception.responseBodyAsString.contains(EXPIRED_MESSAGE, ignoreCase = true) ->
                AuthErrorCode.OAUTH_TOKEN_EXPIRED

            exception.statusCode == HttpStatus.UNAUTHORIZED -> AuthErrorCode.OAUTH_AUTHENTICATION_FAILED
            exception.statusCode == HttpStatus.TOO_MANY_REQUESTS -> AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE
            exception.statusCode.is4xxClientError -> AuthErrorCode.OAUTH_AUTHENTICATION_FAILED
            else -> AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE
        }
        return GeneralException(errorCode)
    }

    private data class KakaoUserResponse(
        val id: Long,
        @JsonProperty("kakao_account") val kakaoAccount: KakaoAccount? = null,
    ) {
        fun toOAuthUserInfo(): OAuthUserInfo =
            OAuthUserInfo(
                providerUserId = id.toString(),
                email = kakaoAccount?.verifiedEmail(),
                name = kakaoAccount?.profile?.nickname,
            )
    }

    private data class KakaoAccount(
        val email: String? = null,
        @JsonProperty("is_email_valid") val isEmailValid: Boolean? = null,
        @JsonProperty("is_email_verified") val isEmailVerified: Boolean? = null,
        val profile: KakaoProfile? = null,
    ) {
        fun verifiedEmail(): String? =
            email?.takeIf { isEmailValid == true && isEmailVerified == true }
    }

    private data class KakaoProfile(
        val nickname: String? = null,
    )

    private companion object {
        const val USER_INFO_PATH = "/v2/user/me"
        const val EXPIRED_MESSAGE = "expired"
    }
}
