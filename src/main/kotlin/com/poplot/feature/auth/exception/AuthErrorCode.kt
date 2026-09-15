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
}
