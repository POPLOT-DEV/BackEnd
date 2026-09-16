package com.poplot.feature.auth.entity

import com.poplot.feature.user.entity.User
import com.poplot.global.common.entity.BaseCreatedTimeEntity
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(
    name = "auth_session",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_auth_session_refresh_token_hash",
            columnNames = ["refresh_token_hash"],
        ),
    ],
)
class AuthSession(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Column(name = "refresh_token_hash", nullable = false, length = 255)
    val refreshTokenHash: String,

    @Column(name = "expires_at", nullable = false)
    val expiresAt: Instant,

    @Column(name = "device_id", length = 255)
    var deviceId: String? = null,

    @Column(name = "device_name", length = 255)
    var deviceName: String? = null,

    @Column(name = "revoked_at")
    var revokedAt: Instant? = null,

    @Column(name = "last_used_at")
    var lastUsedAt: Instant? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auth_session_id")
    val id: Long? = null,
) : BaseCreatedTimeEntity() {
    fun revoke(usedAt: Instant) {
        lastUsedAt = usedAt
        revokedAt = usedAt
    }
}
