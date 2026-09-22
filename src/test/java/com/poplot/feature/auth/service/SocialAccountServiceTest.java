package com.poplot.feature.auth.service;

import com.poplot.feature.auth.entity.SocialAccount;
import com.poplot.feature.auth.enums.SocialProvider;
import com.poplot.feature.auth.exception.AuthErrorCode;
import com.poplot.feature.auth.oauth.OAuthUserInfo;
import com.poplot.feature.auth.repository.SocialAccountRepository;
import com.poplot.feature.user.entity.User;
import com.poplot.feature.user.enums.UserStatus;
import com.poplot.feature.user.repository.UserRepository;
import com.poplot.global.config.JpaAuditingConfig;
import com.poplot.global.exception.GeneralException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import({JpaAuditingConfig.class, SocialAccountService.class, SocialAccountRegistrationService.class})
class SocialAccountServiceTest {
    @Autowired
    private SocialAccountService socialAccountService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SocialAccountRepository socialAccountRepository;

    @Test
    @DisplayName("첫 소셜 로그인 시 사용자와 소셜 계정을 하나씩 생성한다")
    void firstSocialLoginCreatesOneUserAndSocialAccount() {
        OAuthUserInfo userInfo = new OAuthUserInfo("kakao-user-id", "user@example.com", "은우");

        User user = socialAccountService.findOrCreateUser(SocialProvider.KAKAO, userInfo);

        assertThat(user.getId()).isNotNull();
        assertThat(user.getEmail()).isEqualTo("user@example.com");
        assertThat(user.getName()).isEqualTo("은우");
        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(socialAccountRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 계정으로 재로그인해도 기존 사용자를 반환하고 중복 생성하지 않는다")
    void repeatedSocialLoginReturnsExistingUserWithoutDuplicates() {
        User first = socialAccountService.findOrCreateUser(
            SocialProvider.APPLE,
            new OAuthUserInfo("apple-user-id", "first@example.com", null));

        User second = socialAccountService.findOrCreateUser(
            SocialProvider.APPLE,
            new OAuthUserInfo("apple-user-id", "changed@example.com", "changed"));

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(second.getEmail()).isEqualTo("first@example.com");
        assertThat(userRepository.count()).isEqualTo(1);
        assertThat(socialAccountRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("비활성 사용자의 소셜 로그인은 거부된다")
    void inactiveSocialUserIsRejected() {
        User user = userRepository.save(User.builder().status(UserStatus.INACTIVE).build());
        socialAccountRepository.saveAndFlush(new SocialAccount(user, SocialProvider.KAKAO, "inactive-user-id", null));

        assertAuthError(AuthErrorCode.INACTIVE_USER, () ->
            socialAccountService.findOrCreateUser(SocialProvider.KAKAO, new OAuthUserInfo("inactive-user-id", null, null)));
    }

    @Test
    @DisplayName("DB 컬럼 길이를 초과하는 provider user id는 저장 전에 거부된다")
    void providerUserIdExceedingDatabaseLimitIsRejectedBeforePersistence() {
        OAuthUserInfo userInfo = new OAuthUserInfo("a".repeat(256), null, null);

        assertAuthError(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED, () ->
            socialAccountService.findOrCreateUser(SocialProvider.KAKAO, userInfo));
        assertThat(userRepository.count()).isZero();
        assertThat(socialAccountRepository.count()).isZero();
    }

    private void assertAuthError(AuthErrorCode expected, Runnable action) {
        assertThatThrownBy(action::run)
            .isInstanceOfSatisfying(GeneralException.class, exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }
}
