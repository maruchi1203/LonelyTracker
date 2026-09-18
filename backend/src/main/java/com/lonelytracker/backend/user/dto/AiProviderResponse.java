package com.lonelytracker.backend.user.dto;

import com.lonelytracker.backend.user.entity.AiProviderEntity;

/**
 * 제공자 설정 한 줄. 키 원본은 싣지 않는다
 *
 * @param masked 끝 네 자리만. 예: {@code "****abcd"}
 * @param active 지금 이 제공자 설정으로 부르는지
 * @param monthlyTokenLimit 이번 달 토큰 한도. null 이면 한도 없음
 */
public record AiProviderResponse(Long id, String baseUrl, String model, String masked, boolean active,
                                 Integer monthlyTokenLimit) {

    private static final int VISIBLE_TAIL = 4;

    public static AiProviderResponse of(AiProviderEntity provider, Long activeId) {
        return new AiProviderResponse(
                provider.getId(),
                provider.getBaseUrl(),
                provider.getModel(),
                mask(provider.getApiKey()),
                provider.getId().equals(activeId),
                provider.getMonthlyTokenLimit());
    }

    private static String mask(String apiKey) {
        String tail = (apiKey.length() <= VISIBLE_TAIL)
                ? apiKey : apiKey.substring(apiKey.length() - VISIBLE_TAIL);
        return "****" + tail;
    }
}
