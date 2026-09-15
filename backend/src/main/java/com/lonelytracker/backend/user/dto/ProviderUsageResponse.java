package com.lonelytracker.backend.user.dto;

/**
 * 제공자 하나의 기간 사용량
 *
 * @param calls 호출 수
 */
public record ProviderUsageResponse(String baseUrl, Long calls, Long inputTokens, Long outputTokens) {
}
