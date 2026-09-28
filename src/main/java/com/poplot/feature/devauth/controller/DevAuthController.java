package com.poplot.feature.devauth.controller;

import com.poplot.feature.devauth.dto.request.DevTokenRequest;
import com.poplot.feature.devauth.dto.response.DevTokenResponse;
import com.poplot.feature.devauth.service.DevAuthService;
import com.poplot.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile({"local", "dev"})
@RequiredArgsConstructor
@RequestMapping("/dev/auth")
public class DevAuthController {
    private final DevAuthService devAuthService;

    @PostMapping("/token")
    public ApiResponse<DevTokenResponse> issueAccessToken(@RequestBody(required = false) DevTokenRequest request) {
        return ApiResponse.success("COMMON200", "요청에 성공하였습니다.", devAuthService.issueAccessToken(request));
    }
}
