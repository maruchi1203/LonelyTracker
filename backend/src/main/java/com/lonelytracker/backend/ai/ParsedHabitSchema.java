package com.lonelytracker.backend.ai;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 습관 초안을 받을 JSON 스키마.
 *
 * <p>일정 스키마와 가장 다른 점은 카테고리다. 사용자가 가진 이름을 {@code enum} 으로 박아
 * 모델이 그 밖을 고를 수 없게 한다 — 태그처럼 지어 붙이게 두면 없는 카테고리가 와서
 * 저장할 자리가 없어진다. 그래서 이 스키마는 상수가 아니라 요청마다 새로 만든다.
 */
final class ParsedHabitSchema implements AiSchema {

    /** 문장 하나에서 받을 수 있는 습관의 최대 개수. 응답 길이가 곧 토큰 비용이다 */
    static final int MAX_HABITS = 5;

    private final List<String> categories;

    ParsedHabitSchema(List<String> categories) {
        this.categories = categories;
    }

    @Override
    public String name() {
        return "parsed_habits";
    }

    @Override
    public Map<String, Object> strictRoot() {
        Map<String, Object> habits = new LinkedHashMap<>(Map.of(
                "type", "array",
                "description", "문장에서 읽어낸 습관들. 하나뿐이면 길이 1의 배열",
                "items", habit(true)));
        habits.put("maxItems", MAX_HABITS);

        return object(Map.of("habits", habits), List.of("habits"));
    }

    @Override
    public Map<String, Object> optionalFieldsRoot() {
        return object(
                Map.of("habits", Map.of(
                        "type", "array",
                        "description", "문장에서 읽어낸 습관들. 하나뿐이면 길이 1의 배열",
                        "items", habit(false))),
                List.of("habits"));
    }

    private Map<String, Object> habit(boolean nullUnions) {
        return object(
                Map.of(
                        "title", maybeString("습관 이름. 짧은 행동으로 적는다", nullUnions),
                        "categoryName", category(nullUnions),
                        "atTime", maybeString(
                                "언제 할지. \"07:00\" 같은 시각도, \"퇴근 후\" 같은 상황도 그대로 적는다. "
                                        + "문장에 없으면 비운다",
                                nullUnions),
                        "place", maybeString("어디서 하는지. 문장에 없으면 비운다", nullUnions),
                        "twoMinuteAction", maybeString(
                                "2분 안에 끝나는 첫 동작. 문장에 없으면 그 습관을 시작하게 만드는 "
                                        + "가장 작은 동작을 하나 지어 적는다. 예: 운동 → 매트 깔기",
                                nullUnions),
                        "suggestedAction", Map.of(
                                "type", "boolean",
                                "description", "twoMinuteAction 을 문장에서 읽은 것이 아니라 "
                                        + "지어 적었으면 true"),
                        "questions", questions()),
                nullUnions
                        ? List.of("title", "categoryName", "atTime", "place",
                                "twoMinuteAction", "suggestedAction", "questions")
                        : List.of("suggestedAction", "questions"));
    }

    /**
     * 카테고리 칸. 사용자가 가진 이름만 고를 수 있다.
     *
     * <p>이름이 하나도 없는 일은 없다 — 테이블을 심을 때 여섯을 넣고 마지막 하나는 지우지
     * 못하게 막아 두었다. 그래도 빈 목록이 오면 enum 없는 문자열로 두어 호출은 살린다.
     */
    private Map<String, Object> category(boolean nullUnions) {
        String description = "이 습관이 들어갈 카테고리. 아래 목록 중 가장 가까운 하나를 고른다. "
                + "고를 수 없으면 비우고 questions 에 CATEGORY 를 넣는다";

        if (categories.isEmpty()) {
            return maybeString(description, nullUnions);
        }

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", nullUnions ? List.of("string", "null") : "string");
        schema.put("description", description);
        schema.put("enum", enumValues(nullUnions));
        return schema;
    }

    /**
     * 고를 수 있는 값.
     * null 유니온을 쓰는 규약에서는 null 도 열거에 넣어야 "못 골랐다"를 표현할 수 있다
     */
    private List<Object> enumValues(boolean nullUnions) {
        List<Object> values = new java.util.ArrayList<>(categories);
        if (nullUnions) {
            values.add(null);
        }
        return values;
    }

    private static Map<String, Object> questions() {
        return Map.of(
                "type", "array",
                "description", "채우지 못한 칸. 아는 ID 만 고른다",
                "items", Map.of(
                        "type", "string",
                        "enum", Arrays.stream(ParseQuestion.values()).map(Enum::name).toList()));
    }

    // --- Helper ---

    private static Map<String, Object> object(Map<String, Object> properties,
            List<String> required) {
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
