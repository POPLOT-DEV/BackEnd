package com.poplot.feature.devauth.service;

import com.poplot.feature.auth.exception.AuthErrorCode;
import com.poplot.feature.devauth.dto.request.DevTokenRequest;
import com.poplot.feature.devauth.dto.response.DevTokenResponse;
import com.poplot.feature.devauth.exception.DevAuthErrorCode;
import com.poplot.feature.user.entity.User;
import com.poplot.feature.user.enums.UserStatus;
import com.poplot.feature.user.exception.UserErrorCode;
import com.poplot.feature.user.repository.UserRepository;
import com.poplot.global.exception.ErrorCode;
import com.poplot.global.exception.GeneralException;
import com.poplot.global.security.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Profile({"local", "dev"})
@RequiredArgsConstructor
public class DevAuthService {
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    @Transactional(readOnly = true)
    public DevTokenResponse issueAccessToken(DevTokenRequest request) {
        if (request == null || request.userPublicId() == null || request.userPublicId().isBlank()) {
            throw new GeneralException(DevAuthErrorCode.USER_PUBLIC_ID_REQUIRED);
        }

        UUID publicId;
        try {
            publicId = UUID.fromString(request.userPublicId());
        } catch (IllegalArgumentException exception) {
            throw new GeneralException(ErrorCode.INVALID_INPUT_VALUE);
        }

        User user = userRepository.findByPublicId(publicId).orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new GeneralException(AuthErrorCode.INACTIVE_USER);
        }

        return new DevTokenResponse(jwtProvider.createAccessToken(user.getId(), user.getRole().name()), jwtProvider.getAccessTokenExpirationSeconds());
    }
}
