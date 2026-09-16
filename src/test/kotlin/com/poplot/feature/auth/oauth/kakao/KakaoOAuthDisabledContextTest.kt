package com.poplot.feature.auth.oauth.kakao

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class KakaoOAuthDisabledContextTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withUserConfiguration(
                KakaoOAuthConfig::class.java,
                KakaoOAuthProviderClient::class.java,
            ).withPropertyValues("oauth.kakao.enabled=false")

    @Test
    fun `카카오 OAuth가 비활성화되면 관련 빈을 등록하지 않는다`() {
        contextRunner.run { context ->
            assertThat(context).doesNotHaveBean(KakaoOAuthProviderClient::class.java)
            assertThat(context).doesNotHaveBean("kakaoRestClient")
        }
    }
}
