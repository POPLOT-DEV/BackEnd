package com.poplot.global.exception

import org.springframework.http.HttpStatus

interface BaseErrorCode {
    val httpStatus: HttpStatus
    val code: String
    val message: String
}
