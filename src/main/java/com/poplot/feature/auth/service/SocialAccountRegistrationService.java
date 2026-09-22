package com.poplot.feature.auth.service;

import com.poplot.feature.auth.entity.SocialAccount;
import com.poplot.feature.auth.enums.SocialProvider;
import com.poplot.feature.auth.oauth.OAuthUserInfo;
import com.poplot.feature.auth.repository.SocialAccountRepository;
import com.poplot.feature.user.entity.User;
import com.poplot.feature.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SocialAccountRegistrationService {
    private static final int EMAIL_MAX_LENGTH = 320;
    private static final int NAME_MAX_LENGTH = 100;
    private final UserRepository userRepository;
    private final SocialAccountRepository socialAccountRepository;

    @Transactional
    public User createIfAbsent(SocialProvider provider, OAuthUserInfo userInfo) {
        return socialAccountRepository.findByProviderAndProviderUserId(provider, userInfo.providerUserId())
            .map(SocialAccount::getUser)
            .orElseGet(() -> create(provider, userInfo));
    }

    private User create(SocialProvider provider, OAuthUserInfo userInfo) {
        String email = withinLength(userInfo.email(), EMAIL_MAX_LENGTH);
        User user = userRepository.save(User.create(email, withinLength(userInfo.name(), NAME_MAX_LENGTH)));
        socialAccountRepository.saveAndFlush(new SocialAccount(user, provider, userInfo.providerUserId(), email));
        return user;
    }

    private String withinLength(String value, int maxLength) {
        return value != null && value.length() <= maxLength ? value : null;
    }
}
