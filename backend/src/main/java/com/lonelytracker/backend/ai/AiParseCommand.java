package com.lonelytracker.backend.ai;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 파싱 한 번에 필요한 것 전부
 * 
 * @param text       AI에게 명령으로 보낼 텍스트
 * @param now        기준 시각
 * @param knownTags  이미 쓴 적 있는 태그. 후보로만 주고 새 태그도 허용한다
 * @param apiKey     사용자의 OpenAI API 키 (개별 저장)
 */
public record AiParseCommand(
        String text,
        LocalDateTime now,
        List<String> knownTags,
        String apiKey) {
}
