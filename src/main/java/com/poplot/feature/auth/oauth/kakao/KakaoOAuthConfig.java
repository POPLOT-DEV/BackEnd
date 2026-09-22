package com.poplot.feature.auth.oauth.kakao;

import com.poplot.feature.auth.oauth.OAuthBaseUrlValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@ConditionalOnProperty(prefix = "oauth.kakao", name = "enabled", havingValue = "true")
public class KakaoOAuthConfig {
    @Bean
    public RestClient kakaoRestClient(@Value("${oauth.kakao.base-url}") String baseUrl,
                                      @Value("${oauth.kakao.connect-timeout}") Duration connectTimeout,
                                      @Value("${oauth.kakao.read-timeout}") Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        return RestClient.builder().baseUrl(OAuthBaseUrlValidator.requireHttpsBaseUrl(baseUrl)).requestFactory(requestFactory).build();
    }
}
