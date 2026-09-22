package com.poplot.feature.auth.dto.response;

import com.poplot.feature.user.entity.User;
import com.poplot.feature.user.enums.UserRole;

import java.util.UUID;

public record UserResponse(UUID userPublicId, String name, String email, UserRole role) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getPublicId(), user.getName(), user.getEmail(), user.getRole());
    }
}
