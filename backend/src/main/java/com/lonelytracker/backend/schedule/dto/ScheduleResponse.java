package com.lonelytracker.backend.schedule.dto;

import com.lonelytracker.backend.schedule.Schedule;
import com.lonelytracker.backend.schedule.ScheduleStatus;

import java.time.LocalDateTime;

/**
 * 엔티티를 그대로 반환하지 않는 이유:
 * open-in-view: false 환경에서 지연 로딩이 직렬화 중에 터지는 것을 막고,
 * 엔티티 필드가 늘어도 API 계약이 흔들리지 않게 하기 위함.
 */
public record ScheduleResponse(
        Long id,
        String title,
        String description,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean allDay,
        ScheduleStatus status,
        String category,
        /** 반복 시리즈에 속하면 그 id. 단일 일정이면 null. 화면에서 반복 표시에 쓴다 */
        Long seriesId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ScheduleResponse from(Schedule schedule) {
        return new ScheduleResponse(
                schedule.getId(),
                schedule.getTitle(),
                schedule.getDescription(),
                schedule.getStartAt(),
                schedule.getEndAt(),
                schedule.isAllDay(),
                schedule.getStatus(),
                schedule.getCategory(),
                // 지연 로딩 프록시라도 식별자는 쿼리 없이 읽힌다
                schedule.getSeries() == null ? null : schedule.getSeries().getId(),
                schedule.getCreatedAt(),
                schedule.getUpdatedAt()
        );
    }
}
