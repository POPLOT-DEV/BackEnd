package com.poplot.feature.auth.oauth;

import com.poplot.feature.auth.oauth.apple.AppleOAuthConfig;
import com.poplot.feature.auth.oauth.kakao.KakaoOAuthConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuthBaseUrlValidatorTest {
    private static final String HTTPS_BASE_URL = "https://oauth.provider.test";
    private static final String HTTP_BASE_URL = "http://oauth.provider.test";
    private static final String HOSTLESS_HTTPS_BASE_URL = "https:/oauth.provider.test";
    private static final Duration TIMEOUT = Duration.ofSeconds(1);

    @Test
    @DisplayName("Kakao OAuth base URL은 HTTPS만 허용한다")
    void kakaoBaseUrlRequiresHttps() {
        KakaoOAuthConfig config = new KakaoOAuthConfig();
        assertThatCode(() -> config.kakaoRestClient(HTTPS_BASE_URL, TIMEOUT, TIMEOUT)).doesNotThrowAnyException();
        assertThatThrownBy(() -> config.kakaoRestClient(HTTP_BASE_URL, TIMEOUT, TIMEOUT)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> config.kakaoRestClient(HOSTLESS_HTTPS_BASE_URL, TIMEOUT, TIMEOUT)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Apple OAuth base URL은 HTTPS만 허용한다")
    void appleBaseUrlRequiresHttps() {
        AppleOAuthConfig config = new AppleOAuthConfig();
        assertThatCode(() -> config.appleRestClient(HTTPS_BASE_URL, TIMEOUT, TIMEOUT)).doesNotThrowAnyException();
        assertThatThrownBy(() -> config.appleRestClient(HTTP_BASE_URL, TIMEOUT, TIMEOUT)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> config.appleRestClient(HOSTLESS_HTTPS_BASE_URL, TIMEOUT, TIMEOUT)).isInstanceOf(IllegalArgumentException.class);
    }
}
