package com.lonelytracker.backend.user.dto;

import com.lonelytracker.backend.common.FieldLengths;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRenameRequest(
        @NotBlank(message = "name은 필수입니다")
        @Size(max = FieldLengths.CATEGORY_NAME, message = "name은 50자를 넘을 수 없습니다")
        @Pattern(regexp = "[^\\\\/:*?\"<>|]+",
                message = "이름에는 \\ / : * ? \" < > | 문자를 쓸 수 없습니다")
        String name
) {
}
