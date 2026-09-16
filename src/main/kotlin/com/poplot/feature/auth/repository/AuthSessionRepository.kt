package com.poplot.feature.auth.repository

import com.poplot.feature.auth.entity.AuthSession
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface AuthSessionRepository : JpaRepository<AuthSession, Long> {
    fun findByRefreshTokenHash(refreshTokenHash: String): AuthSession?

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        select authSession
        from AuthSession authSession
        join fetch authSession.user
        where authSession.refreshTokenHash = :refreshTokenHash
        """,
    )
    fun findByRefreshTokenHashForUpdate(
        @Param("refreshTokenHash") refreshTokenHash: String,
    ): AuthSession?
}
