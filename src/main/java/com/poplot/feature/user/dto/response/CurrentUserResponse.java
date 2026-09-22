package com.poplot.feature.user.dto.response;

import com.poplot.feature.user.entity.User;
import com.poplot.feature.user.enums.UserRole;

import java.util.UUID;

public record CurrentUserResponse(UUID userPublicId, String name, String email, UserRole role) {
    public static CurrentUserResponse from(User user) {
        return new CurrentUserResponse(user.getPublicId(), user.getName(), user.getEmail(), user.getRole());
    }
}
