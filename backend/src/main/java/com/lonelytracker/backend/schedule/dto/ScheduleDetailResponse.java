package com.lonelytracker.backend.schedule.dto;

import com.lonelytracker.backend.schedule.entity.ScheduleEntity;
import com.lonelytracker.backend.schedule.entity.ScheduleRecurEntity;

import java.time.LocalDateTime;

/**
 * 일정 하나. 회차가 아니라 일정 자체의 값이고, 수정 폼이 쓴다.
 * 목록({@link ScheduleResponse})은 회차를 돌려주므로 규칙을 싣지 않는다.
 *
 * @param durationMinutes 종료 시각 대신 저장하는 값. null이면 끝이 없다
 * @param recurrence      null이면 1회성 일정
 */
public record ScheduleDetailResponse(
        Long id,
        String title,
        String description,
        LocalDateTime startAt,
        Integer durationMinutes,
        boolean allDay,
        String category,
        ScheduleRecurrenceResponse recurrence,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    /** @param recur null이면 1회성 일정 */
    public static ScheduleDetailResponse of(ScheduleEntity schedule, ScheduleRecurEntity recur) {
        return new ScheduleDetailResponse(
                schedule.getId(),
                schedule.getTitle(),
                schedule.getDescription(),
                schedule.getStartAt(),
                schedule.getDurationMinutes(),
                schedule.isAllDay(),
                schedule.getCategory(),
                (recur == null) ? null : ScheduleRecurrenceResponse.from(recur),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt());
    }
}
