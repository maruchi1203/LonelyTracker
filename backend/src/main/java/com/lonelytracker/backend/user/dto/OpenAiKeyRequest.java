package com.lonelytracker.backend.user.dto;

import jakarta.validation.constraints.Size;

/**
 * @param apiKey 사용자의 OpenAI API 키. <b>null 이나 빈 값을 주면 등록을 해제한다.</b>
 *               DB 에는 암호화되어 저장되고 어떤 응답에도 원본이 실리지 않는다
 */
public record OpenAiKeyRequest(
        @Size(max = 300, message = "apiKey는 300자를 넘을 수 없습니다")
        String apiKey
) {
}
