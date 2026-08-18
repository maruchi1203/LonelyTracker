package com.lonelytracker.backend.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 마지막 세그먼트만 바꾼다. 위치 이동(부모 변경)은 별개 기능. */
public record CategoryRenameRequest(
        @NotBlank(message = "name은 필수입니다")
        @Size(max = 50, message = "name은 50자를 넘을 수 없습니다")
        String name
) {
}
