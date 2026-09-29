package com.poplot.feature.reservation.controller;

import com.poplot.feature.reservation.dto.request.CreateReservationRequest;
import com.poplot.feature.reservation.dto.response.CreateReservationResponse;
import com.poplot.feature.reservation.dto.response.ReservationDetailResponse;
import com.poplot.feature.reservation.dto.response.ReservationStatusResponse;
import com.poplot.feature.reservation.service.ReservationService;
import com.poplot.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/reservations")
public class ReservationController {
    private final ReservationService reservationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreateReservationResponse> create(
        @RequestBody(required = false) CreateReservationRequest request
    ) {
        return ApiResponse.success(reservationService.create(request));
    }

    @GetMapping("/{reservationPublicId}")
    public ApiResponse<ReservationDetailResponse> getDetail(@PathVariable String reservationPublicId) {
        return ApiResponse.success(reservationService.getDetail(reservationPublicId));
    }

    @PostMapping("/{reservationPublicId}/confirm")
    public ApiResponse<ReservationStatusResponse> confirm(@PathVariable String reservationPublicId) {
        return ApiResponse.success(reservationService.confirm(reservationPublicId));
    }

    @PostMapping("/{reservationPublicId}/cancel")
    public ApiResponse<ReservationStatusResponse> cancel(@PathVariable String reservationPublicId) {
        return ApiResponse.success(reservationService.cancel(reservationPublicId));
    }
}
