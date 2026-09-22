package com.poplot.feature.user.controller;

import com.poplot.feature.user.dto.response.CurrentUserResponse;
import com.poplot.feature.user.service.CurrentUserService;
import com.poplot.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {
    private final CurrentUserService currentUserService;

    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> getCurrentUser() {
        return ApiResponse.success(CurrentUserResponse.from(currentUserService.getCurrentUser()));
    }
}
