package com.lonelytracker.backend.user.dto;

/**
 * 키 등록 여부만 알려준다. <b>키 자체는 절대 돌려주지 않는다.</b>
 * <p>
 * 화면에는 등록됐는지와 끝 네 자리만 보여준다. 전체를 다시 보여줄 이유가 없고,
 * 보여주는 순간 화면 캡처·로그·브라우저 히스토리로 샐 자리가 늘어난다.
 *
 * @param masked 예: {@code "sk-****abcd"}. 등록 전이면 null
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
