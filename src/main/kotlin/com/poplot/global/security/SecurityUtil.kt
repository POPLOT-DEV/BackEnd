package com.poplot.global.security

import com.poplot.global.exception.ErrorCode
import com.poplot.global.exception.GeneralException
import org.springframework.security.core.context.SecurityContextHolder

object SecurityUtil {
    fun getCurrentUserId(): Long {
        val authentication = SecurityContextHolder.getContext().authentication
            ?: throw GeneralException(ErrorCode.UNAUTHORIZED)

        if (!authentication.isAuthenticated || authentication.principal !is Long) {
            throw GeneralException(ErrorCode.UNAUTHORIZED)
        }
        return authentication.principal as Long
    }
}
