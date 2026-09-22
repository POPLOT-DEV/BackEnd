package com.poplot.feature.auth.enums;

import com.poplot.feature.auth.exception.AuthErrorCode;
import com.poplot.global.exception.GeneralException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SocialProviderTest {
    @Test
    @DisplayName("provider is parsed case insensitively")
    void parsesProviderCaseInsensitively() {
        assertThat(SocialProvider.from("kakao")).isEqualTo(SocialProvider.KAKAO);
        assertThat(SocialProvider.from("APPLE")).isEqualTo(SocialProvider.APPLE);
    }

    @Test
    @DisplayName("unsupported provider throws auth error")
    void rejectsUnsupportedProvider() {
        assertThatThrownBy(() -> SocialProvider.from("google"))
            .isInstanceOfSatisfying(GeneralException.class, exception -> assertThat(exception.getErrorCode()).isEqualTo(AuthErrorCode.UNSUPPORTED_OAUTH_PROVIDER));
    }
}
