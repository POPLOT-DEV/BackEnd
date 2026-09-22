package com.poplot.feature.user.service;

import com.poplot.feature.auth.exception.AuthErrorCode;
import com.poplot.feature.user.entity.User;
import com.poplot.feature.user.enums.UserStatus;
import com.poplot.feature.user.exception.UserErrorCode;
import com.poplot.feature.user.repository.UserRepository;
import com.poplot.global.exception.GeneralException;
import com.poplot.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CurrentUserService {
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public User getCurrentUser() {
        User user = userRepository.findById(SecurityUtil.getCurrentUserId()).orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new GeneralException(AuthErrorCode.INACTIVE_USER);
        }
        return user;
    }
}
