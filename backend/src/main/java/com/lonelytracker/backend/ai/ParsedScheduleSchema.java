package com.lonelytracker.backend.ai;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 구조화 출력(Structured Outputs)에 넘길 JSON 스키마.
 * <p>
 * {@code strict: true} 에는 조건이 붙는다 — <b>모든 속성이 required 에 들어가야 하고
 * additionalProperties 가 false 여야 한다.</b> 그런데 startAt·category 처럼
 * 없을 수 있는 값이 있다. "필수인데 없을 수 있다" 는 <b>타입을 nullable 로 선언</b>해
 * 표현한다. 이걸 모르면 "필수 필드인데 왜 빠지지" 로 한참 헤맨다.
 */
final class ParsedScheduleSchema {

    private ParsedScheduleSchema() {
    }

    static Map<String, Object> get() {
        return object(
                Map.of(
                        "title", nullableString("일정 제목. 모르면 null"),
                        "startAt", nullableString("2026-08-25T14:30:00 형식. 타임존 없음"),
                        "endAt", nullableString("종료 시각. 없으면 null"),
                        "allDay", Map.of("type", "boolean"),
                        "category", nullableString("사용자 분류 목록에 있는 이름만. 없으면 null"),
                        "place", nullableString("어디서 하는지. 문장에 없으면 null"),
                        "recurrence", recurrence(),
                        "questions", questions()),
                List.of("title", "startAt", "endAt", "allDay",
                        "category", "place", "recurrence", "questions"));
    }

    private static Map<String, Object> recurrence() {
        Map<String, Object> schema = new LinkedHashMap<>(object(
                Map.of(
                        "freq", Map.of("type", "string", "enum", List.of("DAILY", "WEEKLY")),
                        "byWeekday", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string", "enum", weekdayNames())),
                        "endsOn", nullableString("YYYY-MM-DD. 끝을 안 정했으면 null")),
                List.of("freq", "byWeekday", "endsOn")));

        // 반복이 아니면 통째로 null 이다. 중첩 객체도 nullable 로 선언해야 한다.
        schema.put("type", List.of("object", "null"));
        return schema;
    }

    private static Map<String, Object> questions() {
        return Map.of(
                "type", "array",
                "description", "채우지 못한 칸. 아는 ID 만 고른다",
                "items", Map.of(
                        "type", "string",
                        // enum 으로 걸면 모델이 질문을 지어낼 수 없다
                        "enum", Arrays.stream(ParseQuestion.values()).map(Enum::name).toList()));
    }

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
