package com.lonelytracker.backend.schedule.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * @param to 옮겨갈 시각. 과거로도 옮길 수 있다 —
 *           "어제 못 한 걸 오늘로" 가 실제로 가장 흔한 사용이다
 */
public record SchedulePostponeRequest(
        @NotNull(message = "to는 필수입니다")
        LocalDateTime to
) {
}
