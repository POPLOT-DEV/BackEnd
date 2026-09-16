package com.poplot.feature.auth.controller

import com.poplot.feature.auth.exception.AuthErrorCode
import com.poplot.feature.auth.service.IssuedAuthTokens
import com.poplot.feature.auth.service.OAuthLoginResult
import com.poplot.feature.auth.service.OAuthLoginService
import com.poplot.feature.auth.service.RefreshTokenService
import com.poplot.feature.user.entity.User
import com.poplot.global.exception.GeneralException
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var oauthLoginService: OAuthLoginService

    @MockitoBean
    private lateinit var refreshTokenService: RefreshTokenService

    @Test
    fun `인증 없이 OAuth 로그인 API를 호출할 수 있다`() {
        val user = User(email = "user@example.com", name = "은우", id = 1L)
        `when`(oauthLoginService.login("kakao", "provider-token")).thenReturn(
            OAuthLoginResult(
                tokens = IssuedAuthTokens("access-token", "refresh-token", 3600),
                user = user,
            ),
        )

        mockMvc.post("/auth/oauth/kakao") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"token":"provider-token"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.isSuccess") { value(true) }
            jsonPath("$.code") { value("OK") }
            jsonPath("$.message") { value("요청에 성공했습니다.") }
            jsonPath("$.data.accessToken") { value("access-token") }
            jsonPath("$.data.refreshToken") { value("refresh-token") }
            jsonPath("$.data.user.userPublicId") { value(user.publicId.toString()) }
            jsonPath("$.data.user.name") { value("은우") }
            jsonPath("$.data.user.email") { value("user@example.com") }
            jsonPath("$.data.user.role") { value("USER") }
        }
    }

    @Test
    fun `OAuth 토큰이 없으면 명세 오류를 반환한다`() {
        `when`(oauthLoginService.login("kakao", null))
            .thenThrow(GeneralException(AuthErrorCode.OAUTH_TOKEN_REQUIRED))

        mockMvc.post("/auth/oauth/kakao") {
            contentType = MediaType.APPLICATION_JSON
            content = "{}"
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.isSuccess") { value(false) }
            jsonPath("$.code") { value("AUTH_400_03") }
            jsonPath("$.data") { doesNotExist() }
        }
    }

    @Test
    fun `인증 없이 Refresh Token을 재발급할 수 있다`() {
        `when`(refreshTokenService.refresh("old-refresh-token")).thenReturn(
            IssuedAuthTokens("new-access-token", "new-refresh-token", 3600),
        )

        mockMvc.post("/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"refreshToken":"old-refresh-token"}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.isSuccess") { value(true) }
            jsonPath("$.code") { value("OK") }
            jsonPath("$.data.accessToken") { value("new-access-token") }
            jsonPath("$.data.refreshToken") { value("new-refresh-token") }
            jsonPath("$.data.expiresIn") { value(3600) }
        }
    }

    @Test
    fun `Refresh Token이 없으면 명세 오류를 반환한다`() {
        `when`(refreshTokenService.refresh(null))
            .thenThrow(GeneralException(AuthErrorCode.REFRESH_TOKEN_REQUIRED))

        mockMvc.post("/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = "{}"
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.isSuccess") { value(false) }
            jsonPath("$.code") { value("AUTH_400_01") }
        }
    }
}
