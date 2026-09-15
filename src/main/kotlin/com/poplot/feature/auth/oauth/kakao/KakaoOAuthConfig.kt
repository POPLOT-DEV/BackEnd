package com.poplot.feature.auth.oauth.kakao

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

@Configuration
class KakaoOAuthConfig {
    @Bean
    fun kakaoRestClient(
        @Value("\${oauth.kakao.base-url}") baseUrl: String,
        @Value("\${oauth.kakao.connect-timeout}") connectTimeout: Duration,
        @Value("\${oauth.kakao.read-timeout}") readTimeout: Duration,
    ): RestClient {
        val requestFactory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(connectTimeout)
            setReadTimeout(readTimeout)
        }
        return RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(requestFactory)
            .build()
    }
}
