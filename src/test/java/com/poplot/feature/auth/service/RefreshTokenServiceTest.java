package com.poplot.feature.auth.service;

import com.poplot.feature.auth.entity.AuthSession;
import com.poplot.feature.auth.exception.AuthErrorCode;
import com.poplot.feature.auth.repository.AuthSessionRepository;
import com.poplot.feature.user.entity.User;
import com.poplot.feature.user.enums.UserStatus;
import com.poplot.feature.user.repository.UserRepository;
import com.poplot.global.config.JpaAuditingConfig;
import com.poplot.global.exception.GeneralException;
import com.poplot.global.security.jwt.JwtProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import({JpaAuditingConfig.class, JwtProvider.class, RefreshTokenHasher.class, AuthTokenService.class, RefreshTokenService.class})
class RefreshTokenServiceTest {
    private static final String TEST_JWT_SECRET = "test-jwt-secret-key-for-poplot-backend-1234567890";

    @Autowired
    private RefreshTokenService refreshTokenService;
    @Autowired
    private AuthTokenService authTokenService;
    @Autowired
    private RefreshTokenHasher refreshTokenHasher;
    @Autowired
    private JwtProvider jwtProvider;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AuthSessionRepository authSessionRepository;

    @Test
    @DisplayName("Refresh Token을 교체하고 기존 세션을 폐기한다")
    void rotatesRefreshTokenAndRevokesOldSession() {
        User user = userRepository.save(User.create(null, null));
        IssuedAuthTokens original = authTokenService.issue(user);

        IssuedAuthTokens refreshed = refreshTokenService.refresh(original.refreshToken());

        List<AuthSession> sessions = authSessionRepository.findAll().stream().sorted(Comparator.comparing(AuthSession::getId)).toList();
        assertThat(refreshed.refreshToken()).isNotEqualTo(original.refreshToken());
        assertThat(jwtProvider.isAccessToken(refreshed.accessToken())).isTrue();
        assertThat(jwtProvider.isRefreshToken(refreshed.refreshToken())).isTrue();
        assertThat(refreshed.accessTokenExpiresIn()).isEqualTo(3600L);
        assertThat(sessions).hasSize(2);
        assertThat(sessions.getFirst().getRevokedAt()).isNotNull();
        assertThat(sessions.getFirst().getLastUsedAt()).isEqualTo(sessions.getFirst().getRevokedAt());
        assertThat(sessions.getLast().getRevokedAt()).isNull();
        assertThat(sessions.stream().map(AuthSession::getFamilyId).toList()).containsOnly(sessions.getFirst().getFamilyId());
    }

    @Test
    @DisplayName("이미 사용한 Refresh Token은 다시 사용할 수 없다")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void reusedRefreshTokenRevokesTokenFamily() {
        try {
            IssuedAuthTokens original = authTokenService.issue(userRepository.save(User.create(null, null)));

            IssuedAuthTokens rotated = refreshTokenService.refresh(original.refreshToken());

            assertAuthError(AuthErrorCode.UNUSABLE_REFRESH_TOKEN, () -> refreshTokenService.refresh(original.refreshToken()));
            assertAuthError(AuthErrorCode.UNUSABLE_REFRESH_TOKEN, () -> refreshTokenService.refresh(rotated.refreshToken()));
            assertThat(authSessionRepository.findAll()).allMatch(session -> session.getRevokedAt() != null);
        } finally {
            authSessionRepository.deleteAll();
            userRepository.deleteAll();
        }
    }

    @Test
    @DisplayName("재사용 감지는 다른 로그인 token family를 폐기하지 않는다")
    void reuseDoesNotRevokeOtherLoginFamily() {
        User user = userRepository.save(User.create(null, null));
        IssuedAuthTokens compromised = authTokenService.issue(user);
        authTokenService.issue(user);

        refreshTokenService.refresh(compromised.refreshToken());

        assertAuthError(AuthErrorCode.UNUSABLE_REFRESH_TOKEN, () -> refreshTokenService.refresh(compromised.refreshToken()));

        List<AuthSession> sessions = authSessionRepository.findAll();
        UUID compromisedFamilyId = sessions.stream()
            .filter(session -> session.getRefreshTokenHash().equals(refreshTokenHasher.hash(compromised.refreshToken())))
            .findFirst()
            .orElseThrow()
            .getFamilyId();
        assertThat(sessions.stream().filter(session -> session.getFamilyId().equals(compromisedFamilyId)).toList())
            .allMatch(session -> session.getRevokedAt() != null);
        assertThat(sessions.stream().filter(session -> !session.getFamilyId().equals(compromisedFamilyId)).toList())
            .allMatch(session -> session.getRevokedAt() == null);
    }

    @Test
    @DisplayName("서버에 등록되지 않은 Refresh Token은 사용할 수 없다")
    void unregisteredRefreshTokenIsRejected() {
        assertAuthError(AuthErrorCode.UNUSABLE_REFRESH_TOKEN, () -> refreshTokenService.refresh(jwtProvider.createRefreshToken(999L)));
    }

    @Test
    @DisplayName("Access Token과 손상된 토큰은 유효하지 않은 Refresh Token으로 거부한다")
    void accessAndMalformedTokensAreRejected() {
        User user = userRepository.save(User.create(null, null));

        assertAuthError(AuthErrorCode.INVALID_REFRESH_TOKEN, () -> refreshTokenService.refresh(jwtProvider.createAccessToken(user.getId())));
        assertAuthError(AuthErrorCode.INVALID_REFRESH_TOKEN, () -> refreshTokenService.refresh("invalid-token"));
    }

    @Test
    @DisplayName("만료된 Refresh Token은 만료 오류로 거부한다")
    void expiredRefreshTokenReturnsExpiredError() throws InterruptedException {
        JwtProvider shortLivedJwtProvider = new JwtProvider(TEST_JWT_SECRET, 3_600_000, 1);
        String expiredToken = shortLivedJwtProvider.createRefreshToken(1L);
        Thread.sleep(10);

        assertAuthError(AuthErrorCode.EXPIRED_REFRESH_TOKEN, () -> refreshTokenService.refresh(expiredToken));
    }

    @Test
    @DisplayName("비활성 사용자의 Refresh Token은 사용할 수 없다")
    void inactiveUserRefreshTokenIsRejected() {
        User user = userRepository.save(User.builder().status(UserStatus.INACTIVE).build());
        IssuedAuthTokens issued = authTokenService.issue(user);

        assertAuthError(AuthErrorCode.UNUSABLE_REFRESH_TOKEN, () -> refreshTokenService.refresh(issued.refreshToken()));
    }

    @Test
    @DisplayName("Refresh Token이 없으면 필수 오류를 반환한다")
    void missingRefreshTokenReturnsRequiredError() {
        assertAuthError(AuthErrorCode.REFRESH_TOKEN_REQUIRED, () -> refreshTokenService.refresh(" "));
    }

    private void assertAuthError(AuthErrorCode expected, Runnable action) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(GeneralException.class, exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
