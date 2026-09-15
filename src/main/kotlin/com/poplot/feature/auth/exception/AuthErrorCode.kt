package com.poplot.feature.auth.exception

import com.poplot.global.exception.BaseErrorCode
import org.springframework.http.HttpStatus

enum class AuthErrorCode(
    override val httpStatus: HttpStatus,
    override val code: String,
    override val message: String,
) : BaseErrorCode {
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
    OAUTH_PROVIDER_UNAVAILABLE(
        HttpStatus.SERVICE_UNAVAILABLE,
        "AUTH_503_01",
        "OAuth 인증 서버와 통신할 수 없습니다.",
    ),
}
