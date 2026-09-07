package com.lonelytracker.backend.ai;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 자연어에서 뽑아낸 일정 초안
 *
 * @param place     실행 장소
 * @param questions API 모델에게 정보가 충분치 않을 경우 질문할 목록
 */
public record ParsedSchedule(
        String title,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean allDay,
        List<String> tags,
        String place,
        ParsedRecurringSchedule recurrence,
        List<ParseQuestion> questions) {
}
