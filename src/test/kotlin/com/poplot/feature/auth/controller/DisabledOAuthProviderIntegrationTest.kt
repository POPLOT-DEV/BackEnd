package com.poplot.feature.auth.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@ActiveProfiles("test")
@SpringBootTest(properties = ["oauth.apple.enabled=false"])
@AutoConfigureMockMvc
class DisabledOAuthProviderIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {
    @Test
    fun `비활성화된 OAuth Provider는 가용성 오류를 반환한다`() {
        mockMvc.post("/auth/oauth/apple") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"token":"provider-token"}"""
        }.andExpect {
            status { isServiceUnavailable() }
            jsonPath("$.isSuccess") { value(false) }
            jsonPath("$.code") { value("AUTH_503_01") }
        }
    }
}
