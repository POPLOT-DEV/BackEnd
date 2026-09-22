package com.poplot.feature.auth.oauth.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.poplot.feature.auth.enums.SocialProvider;
import com.poplot.feature.auth.exception.AuthErrorCode;
import com.poplot.feature.auth.oauth.OAuthProviderClient;
import com.poplot.feature.auth.oauth.OAuthUserInfo;
import com.poplot.global.exception.GeneralException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(prefix = "oauth.kakao", name = "enabled", havingValue = "true")
public class KakaoOAuthProviderClient implements OAuthProviderClient {
    private static final Pattern KAKAO_TEMPORARY_ERROR = Pattern.compile("\"code\"\\s*:\\s*-1(?:\\D|$)");
    private final RestClient restClient;
    private final long appId;

    public KakaoOAuthProviderClient(@Qualifier("kakaoRestClient") RestClient restClient, @Value("${oauth.kakao.app-id}") long appId) {
        if (appId <= 0) {
            throw new IllegalArgumentException("Kakao app ID must be positive.");
        }
        this.restClient = restClient;
        this.appId = appId;
    }

    @Override
    public SocialProvider getProvider() {
        return SocialProvider.KAKAO;
    }

    @Override
    public OAuthUserInfo authenticate(String providerToken) {
        if (providerToken.isBlank()) {
            throw new GeneralException(AuthErrorCode.OAUTH_TOKEN_REQUIRED);
        }
        try {
            KakaoTokenInfoResponse tokenInfo = restClient.get().uri("/v1/user/access_token_info")
                .headers(headers -> headers.setBearerAuth(providerToken)).retrieve().body(KakaoTokenInfoResponse.class);
            if (tokenInfo == null) {
                throw new GeneralException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
            }
            tokenInfo.validate(appId);
            KakaoUserResponse user = restClient.get().uri("/v2/user/me")
                .headers(headers -> headers.setBearerAuth(providerToken)).retrieve().body(KakaoUserResponse.class);
            if (user == null || user.id() == null || !user.id().equals(tokenInfo.id())) {
                throw new GeneralException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
            }
            return user.toOAuthUserInfo();
        } catch (HttpStatusCodeException exception) {
            throw mapHttpException(exception);
        } catch (RestClientException exception) {
            throw new GeneralException(AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE);
        }
    }

    private GeneralException mapHttpException(HttpStatusCodeException exception) {
        AuthErrorCode code;
        if (exception.getStatusCode() == HttpStatus.UNAUTHORIZED && exception.getResponseBodyAsString().toLowerCase(Locale.ROOT).contains("expired")) {
            code = AuthErrorCode.OAUTH_TOKEN_EXPIRED;
        } else if (exception.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            code = AuthErrorCode.OAUTH_AUTHENTICATION_FAILED;
        } else if (KAKAO_TEMPORARY_ERROR.matcher(exception.getResponseBodyAsString()).find()
            || exception.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
            code = AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE;
        } else if (exception.getStatusCode().is4xxClientError()) {
            code = AuthErrorCode.OAUTH_AUTHENTICATION_FAILED;
        } else {
            code = AuthErrorCode.OAUTH_PROVIDER_UNAVAILABLE;
        }
        return new GeneralException(code);
    }

    private record KakaoTokenInfoResponse(Long id, @JsonProperty("expires_in") Long expiresIn, @JsonProperty("app_id") Long appId) {
        void validate(long expectedAppId) {
            if (id == null || appId == null || appId != expectedAppId || expiresIn == null) {
                throw new GeneralException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
            }
            if (expiresIn <= 0) {
                throw new GeneralException(AuthErrorCode.OAUTH_TOKEN_EXPIRED);
            }
        }
    }

    private record KakaoUserResponse(Long id, @JsonProperty("kakao_account") KakaoAccount kakaoAccount) {
        OAuthUserInfo toOAuthUserInfo() {
            return new OAuthUserInfo(Long.toString(id), kakaoAccount == null ? null : kakaoAccount.verifiedEmail(),
                kakaoAccount == null || kakaoAccount.profile() == null ? null : kakaoAccount.profile().nickname());
        }
    }

    private record KakaoAccount(String email, @JsonProperty("is_email_valid") Boolean emailValid,
                                @JsonProperty("is_email_verified") Boolean emailVerified, KakaoProfile profile) {
        String verifiedEmail() {
            return Boolean.TRUE.equals(emailValid) && Boolean.TRUE.equals(emailVerified) ? email : null;
        }
    }

    private record KakaoProfile(String nickname) {
    }
}
