package com.poplot.feature.auth.repository

import com.poplot.feature.auth.entity.AuthSession
import org.springframework.data.jpa.repository.JpaRepository

interface AuthSessionRepository : JpaRepository<AuthSession, Long> {
    fun findByRefreshTokenHash(refreshTokenHash: String): AuthSession?
}
