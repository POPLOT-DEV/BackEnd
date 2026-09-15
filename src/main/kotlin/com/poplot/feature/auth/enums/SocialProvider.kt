package com.poplot.feature.auth.enums

import com.poplot.feature.auth.exception.AuthErrorCode
import com.poplot.global.exception.GeneralException

enum class SocialProvider {
    KAKAO,
    APPLE,
    ;

    companion object {
        fun from(value: String): SocialProvider =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: throw GeneralException(AuthErrorCode.UNSUPPORTED_OAUTH_PROVIDER)
    }
}
