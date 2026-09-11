package com.lonelytracker.backend.habit.dto;

/** 준 것으로 통째로 덮어쓴다 */
public record HabitUpdateRequest(
        String title,
        com.lonelytracker.backend.habit.domain.HabitCategory category,
        String twoMinuteAction) {
}
