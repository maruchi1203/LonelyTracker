package com.lonelytracker.backend.ai;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 파싱 한 번에 필요한 것 전부
 * 
 * @param text       AI에게 명령으로 보낼 텍스트
 * @param now        기준 시각
 * @param categories 사용자의 카테고리 목록. 이 안에서만 고르게 한다
 * @param apiKey     사용자의 OpenAI API 키 (개별 저장)
 */
public record CommandForAI(
        String text,
        LocalDateTime now,
        List<String> categories,
        String apiKey) {
}
