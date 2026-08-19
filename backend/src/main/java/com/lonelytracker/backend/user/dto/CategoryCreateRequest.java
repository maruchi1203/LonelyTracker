package com.lonelytracker.backend.user.dto;

import com.lonelytracker.backend.common.FieldLengths;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryCreateRequest(
        @NotBlank(message = "name은 필수입니다")
        @Size(max = FieldLengths.CATEGORY_NAME, message = "name은 50자를 넘을 수 없습니다")
        // 계층이 없어졌으므로 구분자로 쓰이던 역슬래시도 이제 금지 문자다.
        // 허용 목록이 아니라 금지 목록인 이유: 한글·이모지를 일일이 열거할 수 없기 때문.
        @Pattern(regexp = "[^\\\\/:*?\"<>|]+",
                message = "이름에는 \\ / : * ? \" < > | 문자를 쓸 수 없습니다")
        String name,

        @Size(max = FieldLengths.COLOR, message = "color는 20자를 넘을 수 없습니다")
        String color
) {
}
