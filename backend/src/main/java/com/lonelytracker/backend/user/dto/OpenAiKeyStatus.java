package com.lonelytracker.backend.user.dto;

/**
 * 키 등록 여부. 키 자체는 돌려주지 않는다.
 *
 * @param masked 끝 네 자리만. 예: {@code "sk-****abcd"}. 등록 전이면 null
 */
public record OpenAiKeyStatus(boolean registered, String masked) {

    private static final int VISIBLE_TAIL = 4;

    public static OpenAiKeyStatus of(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return new OpenAiKeyStatus(false, null);
        }
        String tail = (apiKey.length() <= VISIBLE_TAIL)
                ? apiKey : apiKey.substring(apiKey.length() - VISIBLE_TAIL);
        return new OpenAiKeyStatus(true, "****" + tail);
    }
}
