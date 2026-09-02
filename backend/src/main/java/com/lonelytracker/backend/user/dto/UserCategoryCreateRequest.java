package com.lonelytracker.backend.user.dto;

import com.lonelytracker.backend.common.FieldLengths;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserCategoryCreateRequest(
        @NotBlank(message = "name은 필수입니다")
        @Size(max = FieldLengths.CATEGORY_NAME, message = "name은 50자를 넘을 수 없습니다")
        // 금지 목록으로 거른다. 한글·이모지를 허용 목록으로 열거할 수 없다
        @Pattern(regexp = "[^\\\\/:*?\"<>|]+",
                message = "이름에는 \\ / : * ? \" < > | 문자를 쓸 수 없습니다")
        String name,

        @Size(max = FieldLengths.COLOR, message = "color는 20자를 넘을 수 없습니다")
        String color
) {
}
