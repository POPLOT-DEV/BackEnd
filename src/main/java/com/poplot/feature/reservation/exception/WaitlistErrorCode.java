package com.poplot.feature.reservation.exception;

import com.poplot.global.exception.BaseErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum WaitlistErrorCode implements BaseErrorCode {
    INVALID_PARTY_SIZE(HttpStatus.BAD_REQUEST, "WAITLIST_400_01", "대기 인원은 1명 이상이어야 합니다."),
    UNAVAILABLE_TIME_SLOT(HttpStatus.BAD_REQUEST, "WAITLIST_400_02", "대기열에 등록할 수 없는 시간대입니다."),
    FORBIDDEN_VIEW(HttpStatus.FORBIDDEN, "WAITLIST_403_01", "해당 대기열을 조회할 권한이 없습니다."),
    FORBIDDEN_PROCESS(HttpStatus.FORBIDDEN, "WAITLIST_403_01", "해당 대기열을 처리할 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "WAITLIST_404_01", "대기열 정보를 찾을 수 없습니다."),
    DUPLICATE_WAITLIST(HttpStatus.CONFLICT, "WAITLIST_409_01", "이미 해당 시간대의 대기열에 등록되어 있습니다."),
    RESERVATION_AVAILABLE(HttpStatus.CONFLICT, "WAITLIST_409_02", "현재 예약 가능한 시간대입니다."),
    OFFER_EXPIRED(HttpStatus.CONFLICT, "WAITLIST_409_03", "예약 제안이 만료되었습니다."),
    INVALID_ACCEPT_STATUS(HttpStatus.CONFLICT, "WAITLIST_409_04", "수락할 수 없는 대기열 상태입니다."),
    INVALID_DECLINE_STATUS(HttpStatus.CONFLICT, "WAITLIST_409_04", "거절할 수 없는 대기열 상태입니다."),
    INSUFFICIENT_CAPACITY(HttpStatus.CONFLICT, "WAITLIST_409_05", "예약 가능한 인원이 부족합니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
