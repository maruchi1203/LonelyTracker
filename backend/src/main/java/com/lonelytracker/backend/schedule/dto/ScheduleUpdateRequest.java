package com.lonelytracker.backend.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ScheduleUpdateRequest(
        @NotBlank(message = "title은 필수입니다")
        @Size(max = 200, message = "title은 200자를 넘을 수 없습니다")
        String title,

        @Size(max = 2000, message = "description은 2000자를 넘을 수 없습니다")
        String description,

        @NotNull(message = "startAt은 필수입니다")
        LocalDateTime startAt,

        LocalDateTime endAt,

        // 원시 boolean이면 JSON에서 생략됐을 때 Jackson이 실패한다. 선택 필드라 래퍼 타입을 쓴다
        Boolean allDay,

        @Size(max = 50, message = "category는 50자를 넘을 수 없습니다")
        String category
) {
}
