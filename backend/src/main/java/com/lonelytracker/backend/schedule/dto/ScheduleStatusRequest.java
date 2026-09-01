package com.lonelytracker.backend.schedule.dto;

import com.lonelytracker.backend.schedule.domain.ScheduleStatus;
import jakarta.validation.constraints.NotNull;

public record ScheduleStatusRequest(
        @NotNull(message = "status는 필수입니다")
        ScheduleStatus status
) {
}
