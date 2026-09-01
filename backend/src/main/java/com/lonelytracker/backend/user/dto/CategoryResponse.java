package com.lonelytracker.backend.user.dto;

import com.lonelytracker.backend.user.entity.UserCategory;

import java.time.LocalDateTime;

/** 사이드바에 필요한 정보. 계층이 없어 parentId, depth, path 가 모두 사라졌다. */
public record CategoryResponse(
        Long id,
        String name,
        String color,
        int displayOrder,
        boolean archived,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CategoryResponse from(UserCategory category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getColor(),
                category.getDisplayOrder(),
                category.isArchived(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
