package com.lonelytracker.backend.category.dto;

import com.lonelytracker.backend.category.Category;

/** 일정 응답에 끼워 넣는 축약형. 목록 렌더링에 필요한 만큼만 담는다. */
public record CategorySummary(
        Long id,
        String name,
        String path,
        String color
) {
    public static CategorySummary from(Category category) {
        if (category == null) {
            return null;
        }
        return new CategorySummary(
                category.getId(),
                category.getName(),
                category.getPath(),
                category.getColor()
        );
    }
}
