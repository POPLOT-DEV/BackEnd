package com.poplot.feature.example.exception

import com.poplot.global.exception.BaseErrorCode
import org.springframework.http.HttpStatus

enum class ExampleErrorCode(
    override val httpStatus: HttpStatus,
    override val code: String,
    override val message: String,
) : BaseErrorCode {
    EXAMPLE_NOT_FOUND(HttpStatus.NOT_FOUND, "EXAMPLE_404", "예제 데이터를 찾을 수 없습니다."),
}
