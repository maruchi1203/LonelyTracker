package com.lonelytracker.backend.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 마지막 세그먼트만 바꾼다. 위치 이동(부모 변경)은 별개 기능. */
public record CategoryRenameRequest(
        @NotBlank(message = "name은 필수입니다")
        @Size(max = 50, message = "name은 50자를 넘을 수 없습니다")
        // 구분자(\)를 포함해 경로로 오해될 만한 문자를 막는다.
        // 허용 목록이 아니라 금지 목록인 이유: 한글·이모지·기호를 일일이 열거하기 어렵고,
        // 새로운 문자를 쓰고 싶을 때마다 패턴을 고쳐야 하기 때문.
        @Pattern(regexp = "[^\\\\/:*?\"<>|]+",
                message = "이름에는 \\ / : * ? \" < > | 문자를 쓸 수 없습니다")
        String name
) {
}
