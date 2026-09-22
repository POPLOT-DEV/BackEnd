package com.poplot.feature.auth.controller;

import com.poplot.feature.auth.exception.AuthErrorCode;
import com.poplot.feature.auth.service.IssuedAuthTokens;
import com.poplot.feature.auth.service.OAuthLoginResult;
import com.poplot.feature.auth.service.OAuthLoginService;
import com.poplot.feature.auth.service.RefreshTokenService;
import com.poplot.feature.user.entity.User;
import com.poplot.feature.user.enums.UserRole;
import com.poplot.feature.user.enums.UserStatus;
import com.poplot.global.exception.GeneralException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private OAuthLoginService oauthLoginService;
    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("인증 없이 OAuth 로그인 API를 호출할 수 있다")
    void oauthLoginIsPublic() throws Exception {
        User user = User.builder().email("user@example.com").name("은우").role(UserRole.USER).status(UserStatus.ACTIVE).publicId(UUID.randomUUID()).id(1L).build();
        when(oauthLoginService.login("kakao", "provider-token"))
            .thenReturn(new OAuthLoginResult(new IssuedAuthTokens("access-token", "refresh-token", 3600), user));

        mockMvc.perform(post("/auth/oauth/kakao").contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"provider-token\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.success").doesNotExist())
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.message").value("요청에 성공했습니다."))
            .andExpect(jsonPath("$.data.accessToken").value("access-token"))
            .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
            .andExpect(jsonPath("$.data.user.userPublicId").value(user.getPublicId().toString()))
            .andExpect(jsonPath("$.data.user.name").value("은우"))
            .andExpect(jsonPath("$.data.user.email").value("user@example.com"))
            .andExpect(jsonPath("$.data.user.role").value("USER"));
    }

    @Test
    @DisplayName("OAuth 토큰이 없으면 명세 오류를 반환한다")
    void missingOauthTokenReturnsContractError() throws Exception {
        when(oauthLoginService.login("kakao", null)).thenThrow(new GeneralException(AuthErrorCode.OAUTH_TOKEN_REQUIRED));

        mockMvc.perform(post("/auth/oauth/kakao").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.isSuccess").value(false))
            .andExpect(jsonPath("$.code").value("AUTH_400_03"))
            .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("인증 없이 Refresh Token을 재발급할 수 있다")
    void refreshIsPublic() throws Exception {
        when(refreshTokenService.refresh("old-refresh-token")).thenReturn(new IssuedAuthTokens("new-access-token", "new-refresh-token", 3600));

        mockMvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{\"refreshToken\":\"old-refresh-token\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
            .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"))
            .andExpect(jsonPath("$.data.expiresIn").value(3600));
    }

    @Test
    @DisplayName("Refresh Token이 없으면 명세 오류를 반환한다")
    void missingRefreshTokenReturnsContractError() throws Exception {
        when(refreshTokenService.refresh(null)).thenThrow(new GeneralException(AuthErrorCode.REFRESH_TOKEN_REQUIRED));

        mockMvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.isSuccess").value(false))
            .andExpect(jsonPath("$.code").value("AUTH_400_01"));
    }
}
