package com.poplot.feature.auth.service

import com.poplot.feature.auth.enums.SocialProvider
import com.poplot.feature.auth.exception.AuthErrorCode
import com.poplot.feature.auth.oauth.OAuthUserInfo
import com.poplot.feature.auth.repository.SocialAccountRepository
import com.poplot.feature.user.entity.User
import com.poplot.feature.user.enums.UserStatus
import com.poplot.global.exception.GeneralException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service

@Service
class SocialAccountService(
    private val socialAccountRepository: SocialAccountRepository,
    private val registrationService: SocialAccountRegistrationService,
) {
    fun findOrCreateUser(
        provider: SocialProvider,
        userInfo: OAuthUserInfo,
    ): User {
        validateProviderUserId(userInfo.providerUserId)
        val user = socialAccountRepository
            .findByProviderAndProviderUserId(provider, userInfo.providerUserId)
            ?.user
            ?: createWithConcurrentLoginRecovery(provider, userInfo)
        return user.ensureActive()
    }

    private fun createWithConcurrentLoginRecovery(
        provider: SocialProvider,
        userInfo: OAuthUserInfo,
    ): User =
        try {
            registrationService.createIfAbsent(provider, userInfo)
        } catch (exception: DataIntegrityViolationException) {
            socialAccountRepository.findByProviderAndProviderUserId(provider, userInfo.providerUserId)
                ?.user
                ?: throw exception
        }

    private fun validateProviderUserId(providerUserId: String) {
        if (providerUserId.length > PROVIDER_USER_ID_MAX_LENGTH) {
            throw GeneralException(AuthErrorCode.OAUTH_AUTHENTICATION_FAILED)
        }
    }

    private fun User.ensureActive(): User {
        if (status != UserStatus.ACTIVE) {
            throw GeneralException(AuthErrorCode.INACTIVE_USER)
        }
        return this
    }

    private companion object {
        const val PROVIDER_USER_ID_MAX_LENGTH = 255
    }
}
