package com.lonelytracker.backend.schedule.dto;

import com.lonelytracker.backend.schedule.Schedule;
import com.lonelytracker.backend.schedule.ScheduleStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 단일 일정과 전개된 반복 회차를 한 타입으로 내려준다.
 * <p>
 * 반복 회차는 행이 없으므로 {@code id} 가 null 이다.
 * 대신 {@code seriesId} + {@code occurrenceDate} 가 식별자 역할을 한다.
 *
 * @param occurrenceDate 규칙이 만들어낸 원래 날짜. 미뤘어도 이 값은 안 바뀐다.
 *                       startAt 과 다르면 그 회차는 옮겨진 것이다
 * @param postponeCount  몇 번 미뤘는지. 코칭 지표
 */
public record ScheduleResponse(
        Long id,
        Long seriesId,
        LocalDate occurrenceDate,
        String title,
        String description,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean allDay,
        ScheduleStatus status,
        String category,
        int postponeCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    /** 단일 일정. seriesId·occurrenceDate 는 없다. */
    public static ScheduleResponse from(Schedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                null,
                null,
                schedule.getTitle(),
                schedule.getDescription(),
                schedule.getStartAt(),
                schedule.getEndAt(),
                schedule.isAllDay(),
                schedule.getStatus(),
                schedule.getCategory(),
                schedule.getPostponeCount(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt()
        );
    }
}
