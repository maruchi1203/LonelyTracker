package com.lonelytracker.backend.user.dto;

import com.lonelytracker.backend.common.FieldLengths;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        /*
         * 로그인은 OAuth로만 받을 예정이라, 제공자가 주는 이메일이나 식별자가 그대로 들어온다.
         * 형식을 우리가 정할 수 없으므로 문자 규칙을 두지 않는다.
         * (한때 영문·숫자만 허용했는데, 그러면 이메일의 @ 와 . 가 전부 거부된다)
         */
        @NotBlank(message = "username은 필수입니다")
        @Size(max = FieldLengths.USERNAME, message = "username은 100자를 넘을 수 없습니다")
        String username,

        @Size(max = FieldLengths.DISPLAY_NAME, message = "displayName은 50자를 넘을 수 없습니다")
        String displayName
) {
}
