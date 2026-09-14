package com.lonelytracker.backend.user.dto;

import com.lonelytracker.backend.common.FieldLengths;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param baseUrl 제공자 주소. 같은 주소가 이미 있으면 그 줄을 고친다
 * @param model   모델 이름. 이름은 제공자마다 다르다
 * @param apiKey  이미 있는 주소를 고칠 때 비우면 기존 키를 그대로 둔다
 */
public record AiCredentialRequest(
        @NotBlank(message = "제공자 주소를 넣어 주세요")
        @Size(max = FieldLengths.AI_BASE_URL, message = "주소가 너무 깁니다")
        String baseUrl,

        @NotBlank(message = "모델을 정해 주세요")
        @Size(max = FieldLengths.AI_MODEL, message = "모델 이름이 너무 깁니다")
        String model,

        @Size(max = 300, message = "apiKey는 300자를 넘을 수 없습니다")
        String apiKey
) {
}
