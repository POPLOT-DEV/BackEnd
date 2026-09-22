package com.poplot.feature.auth.enums;

import com.poplot.feature.auth.exception.AuthErrorCode;
import com.poplot.global.exception.GeneralException;

import java.util.Arrays;

public enum SocialProvider {
    KAKAO, APPLE;

    public static SocialProvider from(String value) {
        return Arrays.stream(values()).filter(provider -> provider.name().equalsIgnoreCase(value)).findFirst()
            .orElseThrow(() -> new GeneralException(AuthErrorCode.UNSUPPORTED_OAUTH_PROVIDER));
    }
}
