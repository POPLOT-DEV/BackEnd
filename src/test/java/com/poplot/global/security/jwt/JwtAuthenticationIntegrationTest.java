package com.poplot.global.security.jwt;

import com.poplot.feature.user.entity.User;
import com.poplot.feature.user.repository.UserRepository;
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
class JwtAuthenticationIntegrationTest {
    private static final String TEST_JWT_SECRET = "test-jwt-secret-key-for-poplot-backend-1234567890";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Authorization 헤더가 없으면 인증 필요 오류를 반환한다")
    void missingAuthorizationReturnsRequired() throws Exception {
        mockMvc.perform(get("/users/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_401_04"));
    }

    @Test
    @DisplayName("형식이 잘못되거나 유효하지 않은 Access Token은 거부한다")
    void invalidAccessTokensAreRejected() throws Exception {
        for (String header : new String[]{"Token invalid-token", "BearerX invalid-token", "Bearer invalid-token"}) {
            mockMvc.perform(get("/users/me").header("Authorization", header))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_401_05"));
        }
    }

    @Test
    @DisplayName("Bearer 인증 스킴은 대소문자를 구분하지 않는다")
    void bearerSchemeIsCaseInsensitive() throws Exception {
        User user = userRepository.save(User.create(null, null));

        mockMvc.perform(get("/users/me").header("Authorization", "bEaReR " + jwtProvider.createAccessToken(user.getId())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.userPublicId").value(user.getPublicId().toString()));
    }

    @Test
    @DisplayName("Refresh Token을 Access Token으로 사용할 수 없다")
    void refreshTokenCannotBeUsedAsAccessToken() throws Exception {
        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + jwtProvider.createRefreshToken(1L)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_401_05"));
    }

    @Test
    @DisplayName("만료된 Access Token은 만료 오류를 반환한다")
    void expiredAccessTokenReturnsExpiredError() throws Exception {
        JwtProvider shortLivedJwtProvider = new JwtProvider(TEST_JWT_SECRET, 1, 3_600_000);
        String expiredToken = shortLivedJwtProvider.createAccessToken(1L);
        Thread.sleep(10);

        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + expiredToken))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_401_06"));
    }
}
