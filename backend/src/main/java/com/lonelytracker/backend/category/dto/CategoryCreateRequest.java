package com.lonelytracker.backend.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 전체 경로로 생성한다. 중간 단계가 없으면 함께 만들어진다. */
public record CategoryCreateRequest(
        @NotBlank(message = "path는 필수입니다")
        @Size(max = 255, message = "path는 255자를 넘을 수 없습니다")
        // 이름과 달리 구분자(\)는 허용한다. 계층을 표현하는 문자이기 때문.
        // 앞뒤 공백이나 빈 세그먼트는 CategoryPath.normalize 가 정리한다.
        @Pattern(regexp = "[^/:*?\"<>|]+",
                message = "경로에는 / : * ? \" < > | 문자를 쓸 수 없습니다")
        String path,

        @Size(max = 20, message = "color는 20자를 넘을 수 없습니다")
        String color
) {
}
