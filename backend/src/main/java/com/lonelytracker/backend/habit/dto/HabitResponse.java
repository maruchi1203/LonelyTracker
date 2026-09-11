package com.lonelytracker.backend.habit.dto;

import com.lonelytracker.backend.habit.domain.HabitCategory;
import com.lonelytracker.backend.habit.entity.HabitEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 습관 한 줄과 최근 기록.
 *
 * @param doneDates 조회 구간 안에서 해낸 날들
 * @param archived  그만둔 습관인지. 목록에서 내려가되 기록은 남는다
 */
public record HabitResponse(
        Long id,
        String title,
        HabitCategory category,
        String twoMinuteAction,
        int displayOrder,
        boolean archived,
        List<LocalDate> doneDates,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static HabitResponse from(HabitEntity h, List<LocalDate> doneDates) {
        return new HabitResponse(
                h.getId(),
                h.getTitle(),
                h.getCategory(),
                h.getTwoMinuteAction(),
                h.getDisplayOrder(),
                h.isArchived(),
                doneDates,
                h.getCreatedAt(),
                h.getUpdatedAt());
    }
}
