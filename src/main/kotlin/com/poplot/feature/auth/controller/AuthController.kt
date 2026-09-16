package com.poplot.feature.auth.controller

import com.poplot.feature.auth.dto.request.OAuthLoginRequest
import com.poplot.feature.auth.dto.response.OAuthLoginResponse
import com.poplot.feature.auth.service.OAuthLoginService
import com.poplot.global.common.response.ApiResponse
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val oauthLoginService: OAuthLoginService,
) {
    @PostMapping("/oauth/{provider}")
    fun loginWithOAuth(
        @PathVariable provider: String,
        @RequestBody request: OAuthLoginRequest,
    ): ApiResponse<OAuthLoginResponse> =
        ApiResponse.success(
            OAuthLoginResponse.from(oauthLoginService.login(provider, request.token)),
        )
}
