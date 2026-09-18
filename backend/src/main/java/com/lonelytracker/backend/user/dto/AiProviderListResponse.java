package com.lonelytracker.backend.user.dto;

import java.util.List;

/**
 * @param providers      등록한 제공자 설정들. 먼저 넣은 것이 앞에 온다
 * @param serverConfigured 고른 것이 없어도 서버 설정(.env)의 키로 부를 수 있는지
 */
public record AiProviderListResponse(List<AiProviderResponse> providers, boolean serverConfigured) {
}
