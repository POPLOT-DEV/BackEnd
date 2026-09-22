package com.poplot.feature.auth.oauth.kakao;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class KakaoOAuthDisabledContextTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(KakaoOAuthConfig.class, KakaoOAuthProviderClient.class)
        .withPropertyValues("oauth.kakao.enabled=false");

    @Test
    @DisplayName("카카오 OAuth가 비활성화되면 관련 빈을 등록하지 않는다")
    void disabledIntegrationDoesNotCreateBeans() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(KakaoOAuthProviderClient.class);
            assertThat(context).doesNotHaveBean("kakaoRestClient");
        });
    }
}
