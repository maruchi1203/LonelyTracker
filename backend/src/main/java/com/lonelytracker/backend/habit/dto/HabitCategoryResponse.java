package com.lonelytracker.backend.habit.dto;

import com.lonelytracker.backend.habit.entity.HabitCategoryEntity;

/**
 * 갈래 한 줄.
 *
 * @param displayOrder 화면에 늘어놓는 차례. 목록은 이미 이 순서로 온다
 */
public record HabitCategoryResponse(Long id, String name, int displayOrder) {

    public static HabitCategoryResponse from(HabitCategoryEntity c) {
        return new HabitCategoryResponse(c.getId(), c.getName(), c.getDisplayOrder());
    }
}
