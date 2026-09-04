package com.lonelytracker.backend.schedule.dto;

/**
 * 습관 화면의 한 줄. 규칙과 최근 성적을 함께 준다.
 * 규칙을 {@link ScheduleDetailResponse} 그대로 품어 수정 폼에 그대로 넘길 수 있다.
 */
public record ScheduleRecurringResponse(
        ScheduleDetailResponse schedule,
        ScheduleStatsResponse recent) {
}
