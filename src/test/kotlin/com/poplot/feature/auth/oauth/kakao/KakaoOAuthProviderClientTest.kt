package com.poplot.feature.auth.oauth.kakao

import com.poplot.feature.auth.exception.AuthErrorCode
import com.poplot.global.exception.GeneralException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest
import org.springframework.web.client.RestClient

class KakaoOAuthProviderClientTest {
    private lateinit var server: MockRestServiceServer
    private lateinit var client: KakaoOAuthProviderClient

    @BeforeEach
    fun setUp() {
        val builder = RestClient.builder().baseUrl(BASE_URL)
        server = MockRestServiceServer.bindTo(builder).build()
        client = KakaoOAuthProviderClient(builder.build())
    }

    @Test
    fun `valid token returns verified kakao user info`() {
        server.expect(requestTo("$BASE_URL/v2/user/me"))
            .andExpect(method(HttpMethod.GET))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer provider-token"))
            .andRespond(
                withSuccess(
                    """{"id":12345,"kakao_account":{"email":"user@example.com","is_email_valid":true,"is_email_verified":true,"profile":{"nickname":"은우"}}}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        val result = client.authenticate("provider-token")

        assertThat(result.providerUserId).isEqualTo("12345")
        assertThat(result.email).isEqualTo("user@example.com")
        assertThat(result.name).isEqualTo("은우")
        server.verify()
    }

    @Test
    fun `unverified email is not used`() {
        server.expect(requestTo("$BASE_URL/v2/user/me"))
            .andRespond(
                withSuccess(
                    """{"id":12345,"kakao_account":{"email":"user@example.com","is_email_valid":true,"is_email_verified":false}}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        assertThat(client.authenticate("provider-token").email).isNull()
    }

    @Test
    fun `expired token response is mapped to expired auth error`() {
        server.expect(requestTo("$BASE_URL/v2/user/me"))
            .andRespond(withUnauthorizedRequest().body("""{"code":-401,"msg":"access token expired"}"""))

        assertAuthError(AuthErrorCode.OAUTH_TOKEN_EXPIRED) {
            client.authenticate("expired-token")
        }
    }

    @Test
    fun `invalid token response is mapped to authentication failure`() {
        server.expect(requestTo("$BASE_URL/v2/user/me"))
            .andRespond(withUnauthorizedRequest().body("""{"code":-401,"msg":"this access token does not exist"}"""))

        assertAuthError(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED) {
            client.authenticate("invalid-token")
        }
    }

    @Test
    fun `provider server failure is mapped to unavailable error`() {
        server.expect(requestTo("$BASE_URL/v2/user/me"))
            .andRespond(withServerError())

        assertAuthError(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE) {
            client.authenticate("provider-token")
        }
    }

    @Test
    fun `provider client error is mapped to authentication failure`() {
        server.expect(requestTo("$BASE_URL/v2/user/me"))
            .andRespond(withBadRequest())

        assertAuthError(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED) {
            client.authenticate("provider-token")
        }
    }

    @Test
    fun `blank token is rejected without provider request`() {
        assertAuthError(AuthErrorCode.OAUTH_TOKEN_REQUIRED) {
            client.authenticate(" ")
        }
        server.verify()
    }

    private fun assertAuthError(
        expected: AuthErrorCode,
        action: () -> Unit,
    ) {
        assertThatThrownBy(action)
            .isInstanceOf(GeneralException::class.java)
            .extracting("errorCode")
            .isEqualTo(expected)
    }

    private companion object {
        const val BASE_URL = "https://kapi.kakao.test"
    }
}
