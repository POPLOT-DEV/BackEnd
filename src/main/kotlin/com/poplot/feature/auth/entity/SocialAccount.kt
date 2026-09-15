package com.poplot.feature.auth.entity

import com.poplot.feature.auth.enums.SocialProvider
import com.poplot.feature.user.entity.User
import com.poplot.global.common.entity.BaseTimeEntity
import jakarta.persistence.*

@Entity
@Table(
    name = "social_account",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_social_account_provider_user",
            columnNames = ["provider", "provider_user_id"],
        ),
    ],
)
class SocialAccount(
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    val user: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val provider: SocialProvider,

    @Column(name = "provider_user_id", nullable = false, length = 255)
    val providerUserId: String,

    @Column(length = 320)
    var email: String? = null,

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "social_account_id")
    val id: Long? = null,
) : BaseTimeEntity()
