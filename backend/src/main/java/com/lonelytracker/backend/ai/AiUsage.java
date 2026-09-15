package com.lonelytracker.backend.ai;

/**
 * 호출 한 번이 쓴 토큰. 응답에 없으면 0 이다
 */
public record AiUsage(int inputTokens, int outputTokens) {

    public static final AiUsage NONE = new AiUsage(0, 0);
}
