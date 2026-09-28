package com.poplot.feature.devauth.exception;

import com.poplot.global.exception.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum DevAuthErrorCode implements BaseErrorCode {
    USER_PUBLIC_ID_REQUIRED(HttpStatus.BAD_REQUEST, "DEV_AUTH_400_01", "사용자 Public ID가 필요합니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
