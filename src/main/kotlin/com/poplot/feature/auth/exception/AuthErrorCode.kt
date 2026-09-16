package com.poplot.feature.auth.exception

import com.poplot.global.exception.BaseErrorCode
import org.springframework.http.HttpStatus

enum class AuthErrorCode(
    override val httpStatus: HttpStatus,
    override val code: String,
    override val message: String,
) : BaseErrorCode {
    REFRESH_TOKEN_REQUIRED(
        HttpStatus.BAD_REQUEST,
        "AUTH_400_01",
        "Refresh Token이 필요합니다.",
    ),
    UNSUPPORTED_OAUTH_PROVIDER(
        HttpStatus.BAD_REQUEST,
        "AUTH_400_02",
        "지원하지 않는 OAuth Provider입니다.",
    ),
    OAUTH_TOKEN_REQUIRED(
        HttpStatus.BAD_REQUEST,
        "AUTH_400_03",
        "OAuth 인증 토큰이 필요합니다.",
    ),
    INVALID_REFRESH_TOKEN(
        HttpStatus.UNAUTHORIZED,
        "AUTH_401_01",
        "유효하지 않은 Refresh Token입니다.",
    ),
    EXPIRED_REFRESH_TOKEN(
        HttpStatus.UNAUTHORIZED,
        "AUTH_401_02",
        "만료된 Refresh Token입니다.",
    ),
    UNUSABLE_REFRESH_TOKEN(
        HttpStatus.UNAUTHORIZED,
        "AUTH_401_03",
        "사용할 수 없는 Refresh Token입니다.",
    ),
    AUTHENTICATION_REQUIRED(
        HttpStatus.UNAUTHORIZED,
        "AUTH_401_04",
        "인증이 필요합니다.",
    ),
    INVALID_ACCESS_TOKEN(
        HttpStatus.UNAUTHORIZED,
        "AUTH_401_05",
        "유효하지 않은 Access Token입니다.",
    ),
    EXPIRED_ACCESS_TOKEN(
        HttpStatus.UNAUTHORIZED,
        "AUTH_401_06",
        "만료된 Access Token입니다.",
    ),
    OAUTH_AUTHENTICATION_FAILED(
        HttpStatus.UNAUTHORIZED,
        "AUTH_401_07",
        "OAuth 인증에 실패했습니다.",
    ),
    OAUTH_TOKEN_EXPIRED(
        HttpStatus.UNAUTHORIZED,
        "AUTH_401_08",
        "만료된 OAuth 인증 토큰입니다.",
    ),
    INACTIVE_USER(
        HttpStatus.FORBIDDEN,
        "AUTH_403_01",
        "비활성화된 사용자입니다.",
    ),
    OAUTH_PROVIDER_UNAVAILABLE(
        HttpStatus.SERVICE_UNAVAILABLE,
        "AUTH_503_01",
        "OAuth 인증 서버와 통신할 수 없습니다.",
    ),
}
