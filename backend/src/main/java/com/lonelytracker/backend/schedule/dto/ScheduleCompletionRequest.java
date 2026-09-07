package com.lonelytracker.backend.schedule.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 1회성 일정의 완료 여부
 *
 * @param completed 풀면 완료 시각이 지워진다
 */
public record ScheduleCompletionRequest(
                @NotNull(message = "completed는 필수입니다") Boolean completed) {
}
