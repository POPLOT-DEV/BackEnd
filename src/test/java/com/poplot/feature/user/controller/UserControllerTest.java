package com.poplot.feature.user.controller;

import com.poplot.feature.user.entity.User;
import com.poplot.feature.user.enums.UserStatus;
import com.poplot.feature.user.repository.UserRepository;
import com.poplot.global.security.jwt.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Access Token으로 현재 사용자 정보를 조회한다")
    void returnsCurrentUserWithAccessToken() throws Exception {
        User user = userRepository.save(User.create("user@example.com", "은우"));

        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + jwtProvider.createAccessToken(user.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.data.userPublicId").value(user.getPublicId().toString()))
            .andExpect(jsonPath("$.data.name").value("은우"))
            .andExpect(jsonPath("$.data.email").value("user@example.com"))
            .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    @DisplayName("인증 정보에 해당하는 사용자가 없으면 찾을 수 없음 오류를 반환한다")
    void unknownUserReturnsNotFound() throws Exception {
        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + jwtProvider.createAccessToken(Long.MAX_VALUE)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("USER_404_01"));
    }

    @Test
    @DisplayName("비활성 사용자는 자신의 정보를 조회할 수 없다")
    void inactiveUserIsRejected() throws Exception {
        User user = userRepository.save(User.builder().status(UserStatus.INACTIVE).build());

        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + jwtProvider.createAccessToken(user.getId())))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("AUTH_403_01"));
    }
}
