package com.poplot.feature.auth.oauth

import com.poplot.feature.auth.oauth.apple.AppleOAuthConfig
import com.poplot.feature.auth.oauth.kakao.KakaoOAuthConfig
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.Duration

class OAuthBaseUrlValidatorTest {
    @Test
    fun `Kakao OAuth base URL은 HTTPS만 허용한다`() {
        assertThatCode {
            KakaoOAuthConfig().kakaoRestClient(HTTPS_BASE_URL, TIMEOUT, TIMEOUT)
        }.doesNotThrowAnyException()
        assertThatThrownBy {
            KakaoOAuthConfig().kakaoRestClient(HTTP_BASE_URL, TIMEOUT, TIMEOUT)
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `Apple OAuth base URL은 HTTPS만 허용한다`() {
        assertThatCode {
            AppleOAuthConfig().appleRestClient(HTTPS_BASE_URL, TIMEOUT, TIMEOUT)
        }.doesNotThrowAnyException()
        assertThatThrownBy {
            AppleOAuthConfig().appleRestClient(HTTP_BASE_URL, TIMEOUT, TIMEOUT)
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    private companion object {
        const val HTTPS_BASE_URL = "https://oauth.provider.test"
        const val HTTP_BASE_URL = "http://oauth.provider.test"
        val TIMEOUT: Duration = Duration.ofSeconds(1)
    }
}
