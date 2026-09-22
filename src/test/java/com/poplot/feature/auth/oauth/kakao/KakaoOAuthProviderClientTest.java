package com.poplot.feature.auth.oauth.kakao;

import com.poplot.feature.auth.exception.AuthErrorCode;
import com.poplot.feature.auth.oauth.OAuthUserInfo;
import com.poplot.global.exception.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

class KakaoOAuthProviderClientTest {
    private static final String BASE_URL = "https://kapi.kakao.test";
    private MockRestServiceServer server;
    private KakaoOAuthProviderClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        client = new KakaoOAuthProviderClient(builder.build(), 1234L);
    }

    @Test
    @DisplayName("valid token returns verified kakao user info")
    void validTokenReturnsVerifiedUserInfo() {
        expectTokenInfo(12345, 1234, 3600);
        server.expect(requestTo(BASE_URL + "/v2/user/me")).andExpect(method(HttpMethod.GET))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer provider-token"))
            .andRespond(withSuccess("{\"id\":12345,\"kakao_account\":{\"email\":\"user@example.com\",\"is_email_valid\":true,\"is_email_verified\":true,\"profile\":{\"nickname\":\"은우\"}}}", MediaType.APPLICATION_JSON));

        OAuthUserInfo info = client.authenticate("provider-token");

        assertThat(info.providerUserId()).isEqualTo("12345");
        assertThat(info.email()).isEqualTo("user@example.com");
        assertThat(info.name()).isEqualTo("은우");
        server.verify();
    }

    @Test
    @DisplayName("unverified email is not used")
    void unverifiedEmailIsIgnored() {
        expectTokenInfo(12345, 1234, 3600);
        server.expect(requestTo(BASE_URL + "/v2/user/me"))
            .andRespond(withSuccess("{\"id\":12345,\"kakao_account\":{\"email\":\"user@example.com\",\"is_email_valid\":true,\"is_email_verified\":false}}", MediaType.APPLICATION_JSON));

        assertThat(client.authenticate("provider-token").email()).isNull();
    }

    @Test
    @DisplayName("expired token response is mapped to expired auth error")
    void expiredResponseMapsToExpiredError() {
        server.expect(requestTo(BASE_URL + "/v1/user/access_token_info")).andRespond(withUnauthorizedRequest().body("{\"code\":-401,\"msg\":\"access token expired\"}"));

        assertAuthError(AuthErrorCode.OAUTH_TOKEN_EXPIRED, () -> client.authenticate("expired-token"));
    }

    @Test
    @DisplayName("invalid token response is mapped to authentication failure")
    void invalidTokenMapsToAuthenticationFailure() {
        server.expect(requestTo(BASE_URL + "/v1/user/access_token_info")).andRespond(withUnauthorizedRequest().body("{\"code\":-401,\"msg\":\"this access token does not exist\"}"));

        assertAuthError(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED, () -> client.authenticate("invalid-token"));
    }

    @Test
    @DisplayName("provider server failure is mapped to unavailable error")
    void serverFailureMapsToUnavailable() {
        expectTokenInfo(12345, 1234, 3600);
        server.expect(requestTo(BASE_URL + "/v2/user/me")).andRespond(withServerError());

        assertAuthError(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE, () -> client.authenticate("provider-token"));
    }

    @Test
    @DisplayName("provider client error is mapped to authentication failure")
    void clientErrorMapsToAuthenticationFailure() {
        expectTokenInfo(12345, 1234, 3600);
        server.expect(requestTo(BASE_URL + "/v2/user/me")).andRespond(withBadRequest());

        assertAuthError(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED, () -> client.authenticate("provider-token"));
    }

    @Test
    @DisplayName("blank token is rejected without provider request")
    void blankTokenDoesNotCallProvider() {
        assertAuthError(AuthErrorCode.OAUTH_TOKEN_REQUIRED, () -> client.authenticate(" "));
        server.verify();
    }

    @Test
    @DisplayName("token issued for another kakao app is rejected")
    void anotherAppTokenIsRejected() {
        expectTokenInfo(12345, 9999, 3600);

        assertAuthError(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED, () -> client.authenticate("provider-token"));
        server.verify();
    }

    @Test
    @DisplayName("token with no remaining lifetime is rejected as expired")
    void expiredTokenInfoIsRejected() {
        expectTokenInfo(12345, 1234, 0);

        assertAuthError(AuthErrorCode.OAUTH_TOKEN_EXPIRED, () -> client.authenticate("provider-token"));
        server.verify();
    }

    @Test
    @DisplayName("different user ids from token and user info are rejected")
    void mismatchedUserIdsAreRejected() {
        expectTokenInfo(12345, 1234, 3600);
        server.expect(requestTo(BASE_URL + "/v2/user/me")).andRespond(withSuccess("{\"id\":99999}", MediaType.APPLICATION_JSON));

        assertAuthError(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED, () -> client.authenticate("provider-token"));
    }

    @Test
    @DisplayName("kakao temporary error is mapped to unavailable error")
    void temporaryErrorMapsToUnavailable() {
        server.expect(requestTo(BASE_URL + "/v1/user/access_token_info")).andRespond(withBadRequest().body("{\"code\": -1,\"msg\":\"Internal error\"}"));

        assertAuthError(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE, () -> client.authenticate("provider-token"));
    }

    @Test
    @DisplayName("token info response without a user id is rejected")
    void missingTokenUserIdIsRejected() {
        server.expect(requestTo(BASE_URL + "/v1/user/access_token_info"))
            .andRespond(withSuccess("{\"expires_in\":3600,\"app_id\":1234}", MediaType.APPLICATION_JSON));

        assertAuthError(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED, () -> client.authenticate("provider-token"));
    }

    @Test
    @DisplayName("user info response without a user id is rejected")
    void missingUserInfoIdIsRejected() {
        expectTokenInfo(12345, 1234, 3600);
        server.expect(requestTo(BASE_URL + "/v2/user/me"))
            .andRespond(withSuccess("{\"kakao_account\":{}}", MediaType.APPLICATION_JSON));

        assertAuthError(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED, () -> client.authenticate("provider-token"));
    }

    private void expectTokenInfo(long userId, long appId, long expiresIn) {
        server.expect(requestTo(BASE_URL + "/v1/user/access_token_info"))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer provider-token"))
            .andRespond(withSuccess("{\"id\":" + userId + ",\"expires_in\":" + expiresIn + ",\"app_id\":" + appId + "}", MediaType.APPLICATION_JSON));
    }

    private void assertAuthError(AuthErrorCode expected, Runnable action) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(GeneralException.class, exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
