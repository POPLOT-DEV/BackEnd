package com.poplot.feature.reservation.exception;

import com.poplot.global.exception.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CheckInErrorCode implements BaseErrorCode {
    QR_TOKEN_REQUIRED(HttpStatus.BAD_REQUEST, "CHECKIN_400_01", "QR 토큰이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "CHECKIN_403_01", "체크인 권한이 없습니다."),
    INVALID_QR_TOKEN(HttpStatus.NOT_FOUND, "CHECKIN_404_01", "유효한 예약을 찾을 수 없습니다."),
    ALREADY_CHECKED_IN(HttpStatus.CONFLICT, "CHECKIN_409_01", "이미 체크인이 완료된 예약입니다."),
    INVALID_RESERVATION_STATUS(HttpStatus.CONFLICT, "CHECKIN_409_02", "체크인할 수 없는 예약 상태입니다."),
    OUTSIDE_CHECK_IN_WINDOW(HttpStatus.CONFLICT, "CHECKIN_409_03", "현재 체크인할 수 없는 시간입니다."),
    TICKET_KEY_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "CHECKIN_503_01", "일시적으로 체크인할 수 없습니다. 잠시 후 다시 시도해 주세요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
