package com.poplot.feature.auth.dto.response

import com.poplot.feature.auth.service.OAuthLoginResult
import com.poplot.feature.user.entity.User
import com.poplot.feature.user.enums.UserRole
import java.util.UUID

data class OAuthLoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: UserResponse,
) {
    companion object {
        fun from(result: OAuthLoginResult): OAuthLoginResponse =
            OAuthLoginResponse(
                accessToken = result.tokens.accessToken,
                refreshToken = result.tokens.refreshToken,
                user = UserResponse.from(result.user),
            )
    }
}

data class UserResponse(
    val userPublicId: UUID,
    val name: String?,
    val email: String?,
    val role: UserRole,
) {
    companion object {
        fun from(user: User): UserResponse =
            UserResponse(
                userPublicId = user.publicId,
                name = user.name,
                email = user.email,
                role = user.role,
            )
    }
}
