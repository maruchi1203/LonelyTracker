package com.lonelytracker.backend.user.dto;

import com.lonelytracker.backend.user.entity.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String username,
        String displayName,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getCreatedAt());
    }
}
