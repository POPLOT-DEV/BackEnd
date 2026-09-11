package com.poplot.global.exception

class GeneralException(
    val errorCode: BaseErrorCode,
) : RuntimeException(errorCode.message)
