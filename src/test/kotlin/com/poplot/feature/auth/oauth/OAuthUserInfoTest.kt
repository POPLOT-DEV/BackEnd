package com.poplot.feature.auth.oauth

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class OAuthUserInfoTest {
    @Test
    fun `provider user id must not be blank`() {
        assertThatThrownBy {
            OAuthUserInfo(providerUserId = " ", email = null, name = null)
        }.isInstanceOf(IllegalArgumentException::class.java)
    }
}
