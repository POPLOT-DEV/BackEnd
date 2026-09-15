package com.poplot.feature.user.entity

import com.poplot.global.common.entity.BaseTimeEntity
import com.poplot.feature.user.enums.UserRole
import com.poplot.feature.user.enums.UserStatus
import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(
    name = "users",
    uniqueConstraints = [UniqueConstraint(name = "uk_users_public_id", columnNames = ["public_id"])],
)
class User(
    @Column(length = 320)
    var email: String? = null,

    @Column(length = 100)
    var name: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: UserRole = UserRole.USER,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: UserStatus = UserStatus.ACTIVE,

    @Column(name = "public_id", nullable = false, updatable = false)
    val publicId: UUID = UUID.randomUUID(),

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    val id: Long? = null,
) : BaseTimeEntity()
