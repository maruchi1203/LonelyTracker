package com.lonelytracker.backend.schedule.dto;

import com.lonelytracker.backend.schedule.domain.ScheduleStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * 회차 하나. 1회성 일정도 같은 모양으로 나오고, 식별자는 id + instanceDate 다.
 *
 * @param instanceDate 규칙이 만든 원래 날짜. 옮겨도 바뀌지 않는다
 * @param recurring      반복 일정의 회차인지. 규칙 자체는 담지 않는다
 */
public record ScheduleResponse(
                Long id,
                LocalDate instanceDate,
                String title,
                String description,
                LocalDateTime startAt,
                LocalDateTime endAt,
                boolean allDay,
                boolean recurring,
                ScheduleStatus status,
                Set<String> tags,
                String place,
                String twoMinuteAction,
                LocalDateTime createdAt,
                LocalDateTime updatedAt) {
}
