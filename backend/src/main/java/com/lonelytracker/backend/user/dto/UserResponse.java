package com.lonelytracker.backend.user.dto;

import com.lonelytracker.backend.user.entity.UserEntity;

import java.time.LocalDateTime;

/** 사용자 정보. API 키는 어떤 경우에도 싣지 않는다 */
public record UserResponse(
        Long id,
        String username,
        String displayName,
        LocalDateTime createdAt
) {
    public static UserResponse from(UserEntity user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getDisplayName(), user.getCreatedAt());
    }
}
