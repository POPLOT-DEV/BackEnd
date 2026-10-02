package com.poplot.feature.reservation.controller;

import com.poplot.feature.reservation.dto.response.AcceptWaitlistResponse;
import com.poplot.feature.reservation.dto.response.DeclineWaitlistResponse;
import com.poplot.feature.reservation.dto.response.WaitlistDetailResponse;
import com.poplot.feature.reservation.service.WaitlistService;
import com.poplot.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/waitlists/{waitlistPublicId}")
public class WaitlistController {
    private final WaitlistService waitlistService;

    @GetMapping
    public ApiResponse<WaitlistDetailResponse> getDetail(@PathVariable String waitlistPublicId) {
        return ApiResponse.success(waitlistService.getDetail(waitlistPublicId));
    }

    @PostMapping("/accept")
    public ApiResponse<AcceptWaitlistResponse> accept(@PathVariable String waitlistPublicId) {
        return ApiResponse.success(waitlistService.accept(waitlistPublicId));
    }

    @PostMapping("/decline")
    public ApiResponse<DeclineWaitlistResponse> decline(@PathVariable String waitlistPublicId) {
        return ApiResponse.success(waitlistService.decline(waitlistPublicId));
    }
}
