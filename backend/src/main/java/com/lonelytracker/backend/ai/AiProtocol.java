package com.lonelytracker.backend.ai;

import tools.jackson.databind.JsonNode;

import java.util.Map;

/**
 * 제공자마다 다른 것만 담는다.
 * 요청 조립과 봉투 해체가 규약을 타고, 그 밖의 재시도·검증·변환은 {@link AiScheduleParser} 가 공유한다.
 */
interface AiProtocol {

    /** base-url 뒤에 붙는 경로 */
    String path();

    /** 키를 싣는 헤더. 규약마다 이름이 다르다 */
    Map<String, String> authHeaders(String apiKey);

    /**
     * 보낼 본문
     *
     * @param systemPrompt 규칙과 예시. 규약마다 싣는 자리가 다르다
     */
    Map<String, Object> body(String model, String systemPrompt, String userText);

    /**
     * 봉투에서 결과 JSON 문자열을 꺼낸다
     *
     * @throws com.lonelytracker.backend.common.exception.AiParseException 결과가 없을 때
     */
    String resultTextOf(JsonNode envelope);

    /** 이 호출이 쓴 토큰. 필드 이름이 규약마다 다르다 */
    AiUsage usageOf(JsonNode envelope);
}
