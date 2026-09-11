package com.poplot.global.exception

data class ValidationError(
    val field: String,
    val message: String?,
)
