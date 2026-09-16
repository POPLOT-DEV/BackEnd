package com.poplot.feature.user.dto.response

import com.poplot.feature.user.entity.User
import com.poplot.feature.user.enums.UserRole
import java.util.UUID

data class CurrentUserResponse(
    val userPublicId: UUID,
    val name: String?,
    val email: String?,
    val role: UserRole,
) {
    companion object {
        fun from(user: User): CurrentUserResponse =
            CurrentUserResponse(
                userPublicId = user.publicId,
                name = user.name,
                email = user.email,
                role = user.role,
            )
    }
}
