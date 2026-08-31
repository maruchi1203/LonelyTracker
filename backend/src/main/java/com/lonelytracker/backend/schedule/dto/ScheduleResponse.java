package com.lonelytracker.backend.schedule.dto;

import com.lonelytracker.backend.schedule.ScheduleStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 회차 하나. 단일 일정도 "1회짜리 일정" 이므로 같은 모양으로 나온다.
 * <p>
 * 식별자는 {@code id + occurrenceDate} 다. 단일이든 반복이든 같다.
 *
 * @param occurrenceDate 규칙이 만들어낸 원래 날짜. 미뤘어도 이 값은 안 바뀐다.
 *                       startAt 과 날짜가 다르면 그 회차는 옮겨진 것이다
 * @param recurring      반복 일정의 회차인지. 규칙 자체는 담지 않는다
 * @param postponeCount  몇 번 미뤘는지. 코칭 지표
 */
public record ScheduleResponse(
        Long id,
        LocalDate occurrenceDate,
        String title,
        String description,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean allDay,
        boolean recurring,
        ScheduleStatus status,
        String category,
        int postponeCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
