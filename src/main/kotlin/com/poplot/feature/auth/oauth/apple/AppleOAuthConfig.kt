package com.poplot.feature.auth.oauth.apple

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

@Configuration
@ConditionalOnProperty(prefix = "oauth.apple", name = ["enabled"], havingValue = "true")
class AppleOAuthConfig {
    @Bean
    fun appleRestClient(
        @Value("\${oauth.apple.base-url}") baseUrl: String,
        @Value("\${oauth.apple.connect-timeout}") connectTimeout: Duration,
        @Value("\${oauth.apple.read-timeout}") readTimeout: Duration,
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
