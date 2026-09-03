package com.lonelytracker.backend.user.dto;

import com.lonelytracker.backend.user.entity.UserCategoryEntity;

import java.time.LocalDateTime;

/** 사이드바에 뿌릴 카테고리 한 건 */
public record UserCategoryResponse(
        Long id,
        String name,
        String color,
        int displayOrder,
        boolean archived,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static UserCategoryResponse from(UserCategoryEntity category) {
        return new UserCategoryResponse(
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
