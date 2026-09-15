package com.poplot.feature.auth.enums

import com.poplot.feature.auth.exception.AuthErrorCode
import com.poplot.global.exception.GeneralException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class SocialProviderTest {
    @Test
    fun `provider is parsed case insensitively`() {
        assertThat(SocialProvider.from("kakao")).isEqualTo(SocialProvider.KAKAO)
        assertThat(SocialProvider.from("APPLE")).isEqualTo(SocialProvider.APPLE)
    }

    @Test
    fun `unsupported provider throws auth error`() {
        assertThatThrownBy { SocialProvider.from("google") }
            .isInstanceOf(GeneralException::class.java)
            .extracting("errorCode")
            .isEqualTo(AuthErrorCode.UNSUPPORTED_OAUTH_PROVIDER)
    }
}
