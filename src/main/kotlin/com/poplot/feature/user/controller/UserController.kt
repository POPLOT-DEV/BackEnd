package com.poplot.feature.user.controller

import com.poplot.feature.user.dto.response.CurrentUserResponse
import com.poplot.feature.user.service.CurrentUserService
import com.poplot.global.common.response.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class UserController(
    private val currentUserService: CurrentUserService,
) {
    @GetMapping("/me")
    fun getCurrentUser(): ApiResponse<CurrentUserResponse> =
        ApiResponse.success(CurrentUserResponse.from(currentUserService.getCurrentUser()))
}
