package com.poplot.feature.user.exception

import com.poplot.global.exception.BaseErrorCode
import org.springframework.http.HttpStatus

enum class UserErrorCode(
    override val httpStatus: HttpStatus,
    override val code: String,
    override val message: String,
) : BaseErrorCode {
    USER_NOT_FOUND(
        HttpStatus.NOT_FOUND,
        "USER_404_01",
        "사용자를 찾을 수 없습니다.",
    ),
}
