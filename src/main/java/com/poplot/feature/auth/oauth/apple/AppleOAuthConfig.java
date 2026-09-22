package com.poplot.feature.auth.oauth.apple;

import com.poplot.feature.auth.oauth.OAuthBaseUrlValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
@ConditionalOnProperty(prefix = "oauth.apple", name = "enabled", havingValue = "true")
public class AppleOAuthConfig {
    @Bean
    public RestClient appleRestClient(@Value("${oauth.apple.base-url}") String baseUrl,
                                      @Value("${oauth.apple.connect-timeout}") Duration connectTimeout,
                                      @Value("${oauth.apple.read-timeout}") Duration readTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        return RestClient.builder().baseUrl(OAuthBaseUrlValidator.requireHttpsBaseUrl(baseUrl)).requestFactory(requestFactory).build();
    }
}
