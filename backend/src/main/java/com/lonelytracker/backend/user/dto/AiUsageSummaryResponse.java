package com.lonelytracker.backend.user.dto;

import java.util.List;

/**
 * @param week  이번 주(월요일부터) 제공자별 사용량. 호출이 많은 제공자가 앞
 * @param month 이번 달 제공자별 사용량. 호출이 많은 제공자가 앞
 */
public record AiUsageSummaryResponse(List<ProviderUsageResponse> week, List<ProviderUsageResponse> month) {
}
