package com.lonelytracker.backend.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 전체 경로로 생성한다. 중간 단계가 없으면 함께 만들어진다. */
public record CategoryCreateRequest(
        @NotBlank(message = "path는 필수입니다")
        @Size(max = 255, message = "path는 255자를 넘을 수 없습니다")
        String path,

        @Size(max = 20, message = "color는 20자를 넘을 수 없습니다")
        String color
) {
}
