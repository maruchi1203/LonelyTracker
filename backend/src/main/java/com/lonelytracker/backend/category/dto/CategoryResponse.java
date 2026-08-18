package com.lonelytracker.backend.category.dto;

import com.lonelytracker.backend.category.Category;

import java.time.LocalDateTime;

/** 사이드바 렌더링에 필요한 정보. 계층은 path와 parentId로 화면에서 조립한다. */
public record CategoryResponse(
        Long id,
        String name,
        String path,
        Long parentId,
        int depth,
        int displayOrder,
        String color,
        boolean collapsed,
        boolean archived,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getPath(),
                category.getParent() == null ? null : category.getParent().getId(),
                com.lonelytracker.backend.category.CategoryPath.segments(category.getPath()).size() - 1,
                category.getDisplayOrder(),
                category.getColor(),
                category.isCollapsed(),
                category.isArchived(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
