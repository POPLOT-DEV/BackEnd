package com.poplot.feature.example.exception;

import com.poplot.global.exception.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ExampleErrorCode implements BaseErrorCode {
    EXAMPLE_NOT_FOUND(HttpStatus.NOT_FOUND, "EXAMPLE_404", "예제 데이터를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
