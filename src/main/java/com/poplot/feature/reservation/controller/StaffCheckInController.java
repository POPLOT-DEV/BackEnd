package com.poplot.feature.reservation.controller;

import com.poplot.feature.reservation.dto.request.CheckInRequest;
import com.poplot.feature.reservation.dto.response.CheckInResponse;
import com.poplot.feature.reservation.service.CheckInService;
import com.poplot.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/staff/check-ins")
public class StaffCheckInController {
    private final CheckInService checkInService;

    @PostMapping
    public ApiResponse<CheckInResponse> checkIn(@RequestBody(required = false) CheckInRequest request) {
        return ApiResponse.success(checkInService.checkIn(request));
    }
}
