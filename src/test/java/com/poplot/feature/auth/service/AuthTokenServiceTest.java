package com.poplot.feature.auth.service;

import com.poplot.feature.auth.entity.AuthSession;
import com.poplot.feature.auth.repository.AuthSessionRepository;
import com.poplot.feature.user.entity.User;
import com.poplot.feature.user.repository.UserRepository;
import com.poplot.global.config.JpaAuditingConfig;
import com.poplot.global.security.jwt.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import({JpaAuditingConfig.class, JwtProvider.class, RefreshTokenHasher.class, AuthTokenService.class})
class AuthTokenServiceTest {
    @Autowired
    private AuthTokenService authTokenService;
    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private RefreshTokenHasher refreshTokenHasher;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthSessionRepository authSessionRepository;

    @Test
    @DisplayName("issuing login tokens persists only the refresh token hash")
    void persistsOnlyRefreshTokenHash() {
        User user = userRepository.save(User.create("user@example.com", null));

        IssuedAuthTokens issued = authTokenService.issue(user);
        AuthSession session = authSessionRepository.findAll().getFirst();

        assertThat(jwtProvider.isAccessToken(issued.accessToken())).isTrue();
        assertThat(jwtProvider.isRefreshToken(issued.refreshToken())).isTrue();
        assertThat(jwtProvider.getUserId(issued.accessToken())).isEqualTo(user.getId());
        assertThat(issued.accessTokenExpiresIn()).isEqualTo(3_600L);
        assertThat(session.getRefreshTokenHash()).isEqualTo(refreshTokenHasher.hash(issued.refreshToken())).doesNotContain(issued.refreshToken()).hasSize(64);
        assertThat(session.getExpiresAt()).isEqualTo(jwtProvider.getExpiration(issued.refreshToken()));
        assertThat(session.getFamilyId()).isNotNull();
        assertThat(session.getRevokedAt()).isNull();
    }

    @Test
    @DisplayName("each login creates a distinct refresh session")
    void eachLoginCreatesDistinctSession() {
        User user = userRepository.save(User.create(null, null));

        IssuedAuthTokens first = authTokenService.issue(user);
        IssuedAuthTokens second = authTokenService.issue(user);

        List<AuthSession> sessions = authSessionRepository.findAll();
        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        assertThat(sessions).hasSize(2);
        assertThat(sessions.stream().map(AuthSession::getFamilyId).toList()).doesNotHaveDuplicates();
    }
}
