package com.poplot.feature.reservation.exception;

import com.poplot.global.exception.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReservationErrorCode implements BaseErrorCode {
    INVALID_PARTY_SIZE(HttpStatus.BAD_REQUEST, "RESERVATION_400_01", "예약 인원은 1명 이상이어야 합니다."),
    UNAVAILABLE_TIME_SLOT(HttpStatus.BAD_REQUEST, "RESERVATION_400_02", "예약할 수 없는 시간대입니다."),
    TIME_SLOT_NOT_FOUND(HttpStatus.NOT_FOUND, "TIMESLOT_404_01", "예약 시간대를 찾을 수 없습니다."),
    INSUFFICIENT_CAPACITY(HttpStatus.CONFLICT, "RESERVATION_409_01", "예약 가능한 인원이 부족합니다."),
    DUPLICATE_RESERVATION(HttpStatus.CONFLICT, "RESERVATION_409_02", "이미 예약한 시간대입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
