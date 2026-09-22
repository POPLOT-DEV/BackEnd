package com.poplot.feature.auth.oauth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuthUserInfoTest {
    @Test
    @DisplayName("provider user id must not be blank")
    void providerUserIdMustNotBeBlank() {
        assertThatThrownBy(() -> new OAuthUserInfo(" ", null, null)).isInstanceOf(IllegalArgumentException.class);
    }
}
