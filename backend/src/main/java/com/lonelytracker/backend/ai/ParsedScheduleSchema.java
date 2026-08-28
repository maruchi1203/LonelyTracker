package com.lonelytracker.backend.ai;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 구조화 출력(Structured Outputs)에 넘길 JSON 스키마.
 * {@code strict: true} 는 모든 속성이 required 이고 additionalProperties 가 false 일 것을 요구한다.
 * 없을 수 있는 값은 타입을 nullable 로 선언해 표현한다.
 */
final class ParsedScheduleSchema {

    private ParsedScheduleSchema() {
    }

    // 1회성 스케줄
    static Map<String, Object> getSchedule() {
        return object(
                Map.of(
                        "title", nullableString("일정 제목. 모르면 null"),
                        "startAt", nullableString("2026-08-25T14:30:00 형식. 타임존 없음"),
                        "endAt", nullableString("종료 시각. 없으면 null"),
                        "allDay", Map.of("type", "boolean"),
                        "category", nullableString("사용자 분류 목록에 있는 이름만. 없으면 null"),
                        "place", nullableString("어디서 하는지. 문장에 없으면 null"),
                        "recurrence", getRecurringSchedule(),
                        "questions", questions()),
                List.of("title", "startAt", "endAt", "allDay",
                        "category", "place", "recurrence", "questions"));
    }

    // 반복 스케줄
    private static Map<String, Object> getRecurringSchedule() {
        Map<String, Object> schema = new LinkedHashMap<>(object(
                Map.of(
                        "freq", Map.of("type", "string", "enum", List.of("DAILY", "WEEKLY")),
                        "byWeekday", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string", "enum", weekdayNames())),
                        "endsOn", nullableString("YYYY-MM-DD. 끝을 안 정했으면 null")),
                List.of("freq", "byWeekday", "endsOn")));

        // 반복이 아니면 통째로 null 이다
        schema.put("type", List.of("object", "null"));
        return schema;
    }

    // 정보 부족 시 오는 AI의 질문
    private static Map<String, Object> questions() {
        return Map.of(
                "type", "array",
                "description", "채우지 못한 칸. 아는 ID 만 고른다",
                "items", Map.of(
                        "type", "string",
                        "enum", Arrays.stream(ParseQuestion.values()).map(Enum::name).toList()));
    }

    // --- Helper ---

    private static List<String> weekdayNames() {
        return Arrays.stream(DayOfWeek.values()).map(Enum::name).toList();
    }

    private static Map<String, Object> object(Map<String, Object> properties, List<String> required) {
        Map<String, Object> schema = new LinkedHashMap<>();

        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", required);
        schema.put("additionalProperties", false);

        return schema;
    }

    private static Map<String, Object> nullableString(String description) {
        return Map.of("type", List.of("string", "null"), "description", description);
    }
}
