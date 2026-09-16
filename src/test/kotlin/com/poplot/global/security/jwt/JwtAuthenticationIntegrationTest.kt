package com.poplot.global.security.jwt

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class JwtAuthenticationIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val jwtProvider: JwtProvider,
) {
    @Test
    fun `Authorization 헤더가 없으면 인증 필요 오류를 반환한다`() {
        mockMvc.get(PROTECTED_ENDPOINT)
            .andExpect {
                status { isUnauthorized() }
                jsonPath("$.code") { value("AUTH_401_04") }
            }
    }

    @Test
    fun `형식이 잘못되거나 유효하지 않은 Access Token은 거부한다`() {
        mockMvc.get(PROTECTED_ENDPOINT) {
            header("Authorization", "Token invalid-token")
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("AUTH_401_05") }
        }

        mockMvc.get(PROTECTED_ENDPOINT) {
            header("Authorization", "Bearer invalid-token")
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("AUTH_401_05") }
        }
    }

    @Test
    fun `Refresh Token을 Access Token으로 사용할 수 없다`() {
        mockMvc.get(PROTECTED_ENDPOINT) {
            header("Authorization", "Bearer ${jwtProvider.createRefreshToken(1L)}")
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("AUTH_401_05") }
        }
    }

    @Test
    fun `만료된 Access Token은 만료 오류를 반환한다`() {
        val shortLivedJwtProvider = JwtProvider(
            secret = TEST_JWT_SECRET,
            accessTokenExpiration = 1,
            refreshTokenExpiration = 3_600_000,
        )
        val expiredToken = shortLivedJwtProvider.createAccessToken(1L)
        Thread.sleep(10)

        mockMvc.get(PROTECTED_ENDPOINT) {
            header("Authorization", "Bearer $expiredToken")
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("AUTH_401_06") }
        }
    }

    private companion object {
        const val PROTECTED_ENDPOINT = "/users/me"
        const val TEST_JWT_SECRET = "test-jwt-secret-key-for-poplot-backend-1234567890"
    }
}
