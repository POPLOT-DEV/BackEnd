package com.poplot.feature.user.entity

import com.poplot.global.common.entity.BaseTimeEntity
import com.poplot.feature.user.enums.UserRole
import com.poplot.feature.user.enums.UserStatus
import jakarta.persistence.*

@Entity
@Table(name = "users")
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    val id: Long? = null,
) : BaseTimeEntity()
