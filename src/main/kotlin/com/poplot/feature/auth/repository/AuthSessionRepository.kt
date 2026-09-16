package com.poplot.feature.auth.repository

import com.poplot.feature.auth.entity.AuthSession
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update AuthSession authSession
        set authSession.revokedAt = :revokedAt
        where authSession.familyId = :familyId
          and authSession.revokedAt is null
        """,
    )
    fun revokeActiveSessionsByFamilyId(
        @Param("familyId") familyId: UUID,
        @Param("revokedAt") revokedAt: Instant,
    ): Int
}
