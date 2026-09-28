package com.poplot.feature.event.exception;

import com.poplot.global.exception.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum EventErrorCode implements BaseErrorCode {
    RESERVATION_DATE_REQUIRED(HttpStatus.BAD_REQUEST, "EVENT_400_01", "예약 날짜가 필요합니다."),
    INVALID_RESERVATION_DATE(HttpStatus.BAD_REQUEST, "EVENT_400_02", "유효하지 않은 예약 날짜입니다."),
    EVENT_NOT_FOUND(HttpStatus.NOT_FOUND, "EVENT_404_01", "이벤트를 찾을 수 없습니다."),
    EVENT_NOT_PUBLIC(HttpStatus.NOT_FOUND, "EVENT_404_02", "조회할 수 없는 이벤트입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
