package com.lonelytracker.backend.user.dto;

import com.lonelytracker.backend.user.entity.AiCredentialEntity;

/**
 * 자격 증명 한 줄. 키 원본은 싣지 않는다
 *
 * @param masked 끝 네 자리만. 예: {@code "****abcd"}
 * @param active 지금 이 자격 증명으로 부르는지
 */
public record AiCredentialResponse(Long id, String baseUrl, String model, String masked, boolean active) {

    private static final int VISIBLE_TAIL = 4;

    public static AiCredentialResponse of(AiCredentialEntity credential, Long activeId) {
        return new AiCredentialResponse(
                credential.getId(),
                credential.getBaseUrl(),
                credential.getModel(),
                mask(credential.getApiKey()),
                credential.getId().equals(activeId));
    }

    private static String mask(String apiKey) {
        String tail = (apiKey.length() <= VISIBLE_TAIL)
                ? apiKey : apiKey.substring(apiKey.length() - VISIBLE_TAIL);
        return "****" + tail;
    }
}
