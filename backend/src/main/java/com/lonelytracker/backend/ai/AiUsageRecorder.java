package com.lonelytracker.backend.ai;

/**
 * AI 호출 한 번의 사용량을 남기는 계약
 */
public interface AiUsageRecorder {

    /**
     * @param baseUrl 부른 제공자 주소
     * @param model   부른 모델
     * @param usage   응답에 실려 온 토큰 수
     */
    void record(String baseUrl, String model, AiUsage usage);
}
