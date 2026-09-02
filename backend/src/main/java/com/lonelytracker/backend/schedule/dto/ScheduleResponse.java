package com.lonelytracker.backend.schedule.dto;

import com.lonelytracker.backend.schedule.domain.ScheduleStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 회차 하나. 1회성 일정도 같은 모양으로 나오고, 식별자는 id + occurrenceDate 다.
 *
 * @param occurrenceDate 규칙이 만든 원래 날짜. 미뤄도 바뀌지 않는다
 * @param recurring      반복 일정의 회차인지. 규칙 자체는 담지 않는다
 * @param postponeCount  연기 횟수
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
                LocalDateTime updatedAt) {
}
