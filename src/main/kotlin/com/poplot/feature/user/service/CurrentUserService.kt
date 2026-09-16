package com.poplot.feature.user.service

import com.poplot.feature.auth.exception.AuthErrorCode
import com.poplot.feature.user.entity.User
import com.poplot.feature.user.enums.UserStatus
import com.poplot.feature.user.exception.UserErrorCode
import com.poplot.feature.user.repository.UserRepository
import com.poplot.global.exception.GeneralException
import com.poplot.global.security.SecurityUtil
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CurrentUserService(
    private val userRepository: UserRepository,
) {
    @Transactional(readOnly = true)
    fun getCurrentUser(): User {
        val user = userRepository.findById(SecurityUtil.getCurrentUserId())
            .orElseThrow { GeneralException(UserErrorCode.USER_NOT_FOUND) }
        if (user.status != UserStatus.ACTIVE) {
            throw GeneralException(AuthErrorCode.INACTIVE_USER)
        }
        return user
    }
}
