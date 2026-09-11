package com.poplot.global.common.response

import com.fasterxml.jackson.annotation.JsonProperty

data class ApiResponse<T>(
    @field:JsonProperty("isSuccess")
    val isSuccess: Boolean,
    val code: String,
    val message: String,
    val data: T?,
) {
    companion object {
        fun <T> success(data: T): ApiResponse<T> =
            ApiResponse(true, "OK", "요청에 성공했습니다.", data)

        fun <T> success(message: String, data: T): ApiResponse<T> =
            ApiResponse(true, "OK", message, data)

        fun success(): ApiResponse<Void> =
            ApiResponse(true, "OK", "요청에 성공했습니다.", null)

        fun failure(code: String, message: String): ApiResponse<Void> =
            ApiResponse(false, code, message, null)

        fun <T> failure(code: String, message: String, data: T): ApiResponse<T> =
            ApiResponse(false, code, message, data)
    }
}
