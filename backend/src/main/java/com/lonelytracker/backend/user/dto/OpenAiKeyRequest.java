package com.lonelytracker.backend.user.dto;

import jakarta.validation.constraints.Size;

/**
 * @param apiKey OpenAI API 키. null이나 빈 값을 주면 등록을 해제한다
 */
public record OpenAiKeyRequest(
        @Size(max = 300, message = "apiKey는 300자를 넘을 수 없습니다")
        String apiKey
) {
}
