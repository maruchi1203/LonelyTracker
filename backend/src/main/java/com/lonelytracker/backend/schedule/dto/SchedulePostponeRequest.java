package com.lonelytracker.backend.schedule.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * @param to 옮겨갈 시각
 */
public record SchedulePostponeRequest(
                @NotNull(message = "to는 필수입니다") LocalDateTime to) {
}
