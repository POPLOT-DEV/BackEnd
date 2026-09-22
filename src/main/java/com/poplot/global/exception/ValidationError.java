package com.poplot.global.exception;

public record ValidationError(String field, String message) {
}
