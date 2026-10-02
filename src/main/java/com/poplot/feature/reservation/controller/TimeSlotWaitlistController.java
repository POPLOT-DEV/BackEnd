package com.poplot.feature.reservation.controller;

import com.poplot.feature.reservation.dto.request.CreateWaitlistRequest;
import com.poplot.feature.reservation.dto.response.CreateWaitlistResponse;
import com.poplot.feature.reservation.service.WaitlistService;
import com.poplot.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/timeslots/{timeSlotPublicId}/waitlists")
public class TimeSlotWaitlistController {
    private final WaitlistService waitlistService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CreateWaitlistResponse> register(
        @PathVariable String timeSlotPublicId,
        @RequestBody(required = false) CreateWaitlistRequest request
    ) {
        return ApiResponse.success(waitlistService.register(timeSlotPublicId, request));
    }
}
