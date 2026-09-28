package com.poplot.feature.devauth.controller;

import com.poplot.feature.user.entity.User;
import com.poplot.feature.user.enums.UserRole;
import com.poplot.feature.user.enums.UserStatus;
import com.poplot.feature.user.repository.UserRepository;
import com.poplot.global.security.jwt.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles({"local", "test"})
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DevAuthControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("local profile issues a public access token with the stored role")
    void issuesAccessTokenWithStoredRole() throws Exception {
        User user = userRepository.save(User.builder().role(UserRole.STAFF).status(UserStatus.ACTIVE).build());

        String response = mockMvc.perform(post("/dev/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userPublicId\":\"" + user.getPublicId() + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isSuccess").value(true))
            .andExpect(jsonPath("$.code").value("COMMON200"))
            .andExpect(jsonPath("$.data.expiresIn").value(3600))
            .andReturn().getResponse().getContentAsString();

        String accessToken = com.jayway.jsonpath.JsonPath.read(response, "$.data.accessToken");
        assertThat(jwtProvider.getUserId(accessToken)).isEqualTo(user.getId());
        assertThat(jwtProvider.parseClaims(accessToken).get("role", String.class)).isEqualTo("STAFF");
    }

    @Test
    @DisplayName("missing public id returns the dev auth contract error")
    void missingPublicIdReturnsContractError() throws Exception {
        mockMvc.perform(post("/dev/auth/token").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("DEV_AUTH_400_01"));
    }

    @Test
    @DisplayName("unknown public id returns user not found")
    void unknownPublicIdReturnsNotFound() throws Exception {
        mockMvc.perform(post("/dev/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userPublicId\":\"" + UUID.randomUUID() + "\"}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("USER_404_01"));
    }

    @Test
    @DisplayName("malformed public id returns a validation error")
    void malformedPublicIdReturnsValidationError() throws Exception {
        mockMvc.perform(post("/dev/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userPublicId\":\"not-a-uuid\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("COMMON_400"));
    }

    @Test
    @DisplayName("inactive user cannot receive a development token")
    void inactiveUserCannotReceiveToken() throws Exception {
        User user = userRepository.save(User.builder().status(UserStatus.INACTIVE).build());

        mockMvc.perform(post("/dev/auth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"userPublicId\":\"" + user.getPublicId() + "\"}"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("AUTH_403_01"));
    }
}
