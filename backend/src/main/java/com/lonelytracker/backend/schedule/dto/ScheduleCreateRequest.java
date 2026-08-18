package com.lonelytracker.backend.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record ScheduleCreateRequest(
        @NotBlank(message = "title은 필수입니다")
        @Size(max = 200, message = "title은 200자를 넘을 수 없습니다")
        String title,

        // 마크다운 원문. 문서처럼 길어질 수 있어 넉넉하게 잡는다
        @Size(max = 20000, message = "description은 20000자를 넘을 수 없습니다")
        String description,

        @NotNull(message = "startAt은 필수입니다")
        LocalDateTime startAt,

        LocalDateTime endAt,

        // 원시 boolean이면 JSON에서 생략됐을 때 Jackson이 실패한다. 선택 필드라 래퍼 타입을 쓴다
        Boolean allDay,

        // 역슬래시로 계층 표현. 예: 능력\개발\SpringBoot
        @Size(max = 100, message = "category는 100자를 넘을 수 없습니다")
        String category
) {
}
