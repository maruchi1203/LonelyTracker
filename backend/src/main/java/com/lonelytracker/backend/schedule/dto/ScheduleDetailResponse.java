package com.lonelytracker.backend.schedule.dto;

import com.lonelytracker.backend.schedule.entity.ScheduleEntity;
import com.lonelytracker.backend.schedule.entity.ScheduleRecurEntity;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 일정 하나. 회차가 아니라 일정 자체의 값이고, 수정 폼이 쓴다.
 * 목록({@link ScheduleResponse})은 회차를 돌려주므로 규칙을 싣지 않는다.
 * <p>
 * 필드는 {@link ScheduleUpdateRequest} 에 읽기 전용 셋(id, createdAt, updatedAt)을 더한 것이다.
 * 그 셋만 빼면 그대로 되돌려 보낼 수 있다.
 *
 * @param endAt      소요시간을 시작에 더한 값. 반복이면 첫 회차의 끝이다. null이면 끝이 없다
 * @param recurrence null이면 1회성 일정
 */
public record ScheduleDetailResponse(
        Long id,
        String title,
        String description,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean allDay,
        String category,
        String place,
        String twoMinuteAction,
        ScheduleRecurrenceResponse recurrence,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    /** @param recur null이면 1회성 일정 */
    public static ScheduleDetailResponse of(ScheduleEntity schedule, ScheduleRecurEntity recur) {
        Integer minutes = schedule.getDurationMinutes();

        return new ScheduleDetailResponse(
                schedule.getId(),
                schedule.getTitle(),
                schedule.getDescription(),
                schedule.getStartAt(),
                (minutes == null) ? null : schedule.getStartAt().plus(Duration.ofMinutes(minutes)),
                schedule.isAllDay(),
                schedule.getCategory(),
                schedule.getPlace(),
                schedule.getTwoMinuteAction(),
                (recur == null) ? null : ScheduleRecurrenceResponse.from(recur),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt());
    }
}
