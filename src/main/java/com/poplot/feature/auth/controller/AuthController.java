package com.poplot.feature.auth.controller;

import com.poplot.feature.auth.dto.request.OAuthLoginRequest;
import com.poplot.feature.auth.dto.request.RefreshTokenRequest;
import com.poplot.feature.auth.dto.response.OAuthLoginResponse;
import com.poplot.feature.auth.dto.response.RefreshTokenResponse;
import com.poplot.feature.auth.service.OAuthLoginService;
import com.poplot.feature.auth.service.RefreshTokenService;
import com.poplot.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    private final OAuthLoginService oauthLoginService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/oauth/{provider}")
    public ApiResponse<OAuthLoginResponse> loginWithOAuth(@PathVariable String provider, @RequestBody OAuthLoginRequest request) {
        return ApiResponse.success(OAuthLoginResponse.from(oauthLoginService.login(provider, request.token())));
    }

    @PostMapping("/refresh")
    public ApiResponse<RefreshTokenResponse> refresh(@RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(RefreshTokenResponse.from(refreshTokenService.refresh(request.refreshToken())));
    }
}
