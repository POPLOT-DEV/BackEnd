package com.poplot.feature.auth.service;

import com.poplot.feature.auth.entity.SocialAccount;
import com.poplot.feature.auth.enums.SocialProvider;
import com.poplot.feature.auth.exception.AuthErrorCode;
import com.poplot.feature.auth.oauth.OAuthUserInfo;
import com.poplot.feature.auth.repository.SocialAccountRepository;
import com.poplot.feature.user.entity.User;
import com.poplot.feature.user.enums.UserStatus;
import com.poplot.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SocialAccountService {
    private static final int PROVIDER_USER_ID_MAX_LENGTH = 255;
    private final SocialAccountRepository socialAccountRepository;
    private final SocialAccountRegistrationService registrationService;

    public User findOrCreateUser(SocialProvider provider, OAuthUserInfo userInfo) {
        if (userInfo.providerUserId().length() > PROVIDER_USER_ID_MAX_LENGTH) {
            throw new GeneralException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED);
        }
        User user = socialAccountRepository.findByProviderAndProviderUserId(provider, userInfo.providerUserId())
            .map(SocialAccount::getUser)
            .orElseGet(() -> createWithConcurrentLoginRecovery(provider, userInfo));
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new GeneralException(AuthErrorCode.INACTIVE_USER);
        }
        return user;
    }

    private User createWithConcurrentLoginRecovery(SocialProvider provider, OAuthUserInfo userInfo) {
        try {
            return registrationService.createIfAbsent(provider, userInfo);
        } catch (DataIntegrityViolationException exception) {
            return socialAccountRepository.findByProviderAndProviderUserId(provider, userInfo.providerUserId())
                .map(SocialAccount::getUser)
                .orElseThrow(() -> exception);
        }
    }
}
