package com.lonelytracker.backend.ai;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 구조화 출력(Structured Outputs)에 넘길 JSON 스키마.
 * {@code strict: true} 는 모든 속성이 required 이고 additionalProperties 가 false 일 것을 요구한다.
 *
 * <p>없을 수 있는 값을 어떻게 적는지가 규약마다 다르다.
 * OpenAI 호환은 타입에 null 을 더한 유니온을 받고, Claude 네이티브는 유니온을 받지 않아
 * 그 칸을 required 에서 빼는 방식만 쓴다. 그래서 뿌리를 두 갈래로 낸다.
 */
final class ParsedScheduleSchema {

    private ParsedScheduleSchema() {
    }

    /** 문장 하나에서 받을 수 있는 일정의 최대 개수. 응답 길이가 곧 토큰 비용이다 */
    static final int MAX_SCHEDULES = 10;

    /**
     * 응답의 뿌리. 한 문장에 일정이 여럿 들어 있을 수 있어 배열로 받는다.
     * 구조화 출력은 뿌리가 object 여야 해서 배열을 한 칸에 담아 감싼다.
     */
    static Map<String, Object> getRoot() {
        Map<String, Object> schedules = new LinkedHashMap<>(Map.of(
                "type", "array",
                "description", "문장에서 읽어낸 일정들. 하나뿐이면 길이 1의 배열",
                "items", getSchedule(true)));
        schedules.put("maxItems", MAX_SCHEDULES);

        return object(Map.of("schedules", schedules), List.of("schedules"));
    }

    /**
     * null 유니온과 배열 상한을 못 쓰는 규약용 뿌리.
     * 없는 값은 칸 자체를 빼고 오므로 개수 상한은 코드가 자른다
     */
    static Map<String, Object> getRootWithOptionalFields() {
        return object(
                Map.of("schedules", Map.of(
                        "type", "array",
                        "description", "문장에서 읽어낸 일정들. 하나뿐이면 길이 1의 배열",
                        "items", getSchedule(false))),
                List.of("schedules"));
    }

    // 1회성 스케줄
    static Map<String, Object> getSchedule(boolean nullUnions) {
        return object(
                Map.of(
                        "title", maybeString("일정 제목. 모르면 비움", nullUnions),
                        "startAt", maybeString("2026-08-25T14:30:00 형식. 타임존 없음", nullUnions),
                        "endAt", maybeString("종료 시각. 없으면 비움", nullUnions),
                        "allDay", Map.of("type", "boolean"),
                        "tags", Map.of(
                                "type", "array",
                                "description", "일정의 분류. 후보에 맞는 것이 있으면 그것을 쓰고, "
                                        + "없으면 한 단어로 새로 짓는다. 되도록 하나는 붙인다",
                                "minItems", 1,
                                "items", Map.of("type", "string")),
                        "place", maybeString("어디서 하는지. 문장에 없으면 비움", nullUnions),
                        "recurrence", getRecurringSchedule(nullUnions),
                        "questions", questions()),
                // 유니온을 못 쓰는 규약에서는 없을 수 있는 칸을 required 에서 뺀다
                nullUnions
                        ? List.of("title", "startAt", "endAt", "allDay",
                                "tags", "place", "recurrence", "questions")
                        : List.of("allDay", "tags", "questions"));
    }

    // 반복 스케줄
    private static Map<String, Object> getRecurringSchedule(boolean nullUnions) {
        Map<String, Object> schema = new LinkedHashMap<>(object(
                Map.of(
                        "freq", Map.of("type", "string", "enum", List.of("DAILY", "WEEKLY"),
                                "description", "며칠 연속이면 DAILY, 특정 요일마다면 WEEKLY"),
                        "byWeekday", Map.of(
                                "type", "array",
                                "items", Map.of("type", "string", "enum", weekdayNames())),
                        "endsOn", maybeString("YYYY-MM-DD. 반복이 끝나는 날. "
                                + "\"~까지\", \"~주간\", \"이번 주\" 처럼 끝이 있으면 그 날짜를 넣는다. "
                                + "끝이 없을 때만 비운다", nullUnions)),
                nullUnions
                        ? List.of("freq", "byWeekday", "endsOn")
                        : List.of("freq", "byWeekday")));

        // 반복이 아니면 통째로 null 이다. 유니온을 못 쓰면 칸 자체가 빠진다
        if (nullUnions) {
            schema.put("type", List.of("object", "null"));
        }
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

    /** 없을 수 있는 문자열 칸. 규약이 유니온을 받으면 null 을 더하고, 아니면 그냥 string 이다 */
    private static Map<String, Object> maybeString(String description, boolean nullUnions) {
        return nullUnions
                ? Map.of("type", List.of("string", "null"), "description", description)
                : Map.of("type", "string", "description", description);
    }
}
