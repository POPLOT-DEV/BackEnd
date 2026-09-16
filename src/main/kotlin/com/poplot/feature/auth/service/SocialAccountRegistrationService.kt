package com.poplot.feature.auth.service

import com.poplot.feature.auth.entity.SocialAccount
import com.poplot.feature.auth.enums.SocialProvider
import com.poplot.feature.auth.oauth.OAuthUserInfo
import com.poplot.feature.auth.repository.SocialAccountRepository
import com.poplot.feature.user.entity.User
import com.poplot.feature.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SocialAccountRegistrationService(
    private val userRepository: UserRepository,
    private val socialAccountRepository: SocialAccountRepository,
) {
    @Transactional
    fun createIfAbsent(
        provider: SocialProvider,
        userInfo: OAuthUserInfo,
    ): User {
        socialAccountRepository.findByProviderAndProviderUserId(provider, userInfo.providerUserId)
            ?.let { return it.user }

        val user = userRepository.save(
            User(
                email = userInfo.email?.takeIf { it.length <= EMAIL_MAX_LENGTH },
                name = userInfo.name?.takeIf { it.length <= NAME_MAX_LENGTH },
            ),
        )
        socialAccountRepository.saveAndFlush(
            SocialAccount(
                user = user,
                provider = provider,
                providerUserId = userInfo.providerUserId,
                email = userInfo.email?.takeIf { it.length <= EMAIL_MAX_LENGTH },
            ),
        )
        return user
    }

    private companion object {
        const val EMAIL_MAX_LENGTH = 320
        const val NAME_MAX_LENGTH = 100
    }
}
