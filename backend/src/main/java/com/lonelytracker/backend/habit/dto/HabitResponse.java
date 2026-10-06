package com.lonelytracker.backend.habit.dto;

import com.lonelytracker.backend.habit.entity.HabitEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 습관 한 줄과 최근 기록.
 *
 * @param categoryId 묶여 있는 갈래. 이름은 싣지 않는다 — 화면이 갈래 목록을 따로 들고 있고,
 *                   거기에는 습관이 하나도 없는 갈래까지 들어 있어야 한다
 * @param doneDates  조회 구간 안에서 해낸 날들
 * @param archived   그만둔 습관인지. 목록에서 내려가되 기록은 남는다
 */
public record HabitResponse(
        Long id,
        String title,
        Long categoryId,
        String twoMinuteAction,
        String atTime,
        String place,
        int displayOrder,
        boolean archived,
        List<LocalDate> doneDates,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static HabitResponse from(HabitEntity h, List<LocalDate> doneDates) {
        return new HabitResponse(
                h.getId(),
                h.getTitle(),
                h.getCategory().getId(),
                h.getTwoMinuteAction(),
                h.getAtTime(),
                h.getPlace(),
                h.getDisplayOrder(),
                h.isArchived(),
                doneDates,
                h.getCreatedAt(),
                h.getUpdatedAt());
    }
}
