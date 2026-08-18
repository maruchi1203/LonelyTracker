package com.lonelytracker.backend.category;

import java.util.Arrays;
import java.util.List;

/**
 * 카테고리는 역슬래시로 계층을 표현한다. 예: {@code 능력\개발\SpringBoot}
 * <p>
 * DB에서는 parent_id 로 계층을 잡지만, 조회와 입력은 이 경로 문자열을 쓴다.
 */
public final class CategoryPath {

    /** 계층 구분자. 자바 문자열이라 "\\" 가 역슬래시 한 글자다. */
    public static final String SEPARATOR = "\\";

    /**
     * LIKE 패턴의 이스케이프 문자.
     * PostgreSQL은 LIKE에서 역슬래시를 기본 이스케이프로 취급하는데,
     * 우리 구분자가 역슬래시라 충돌한다. 다른 문자를 명시적으로 지정해
     * 구분자가 리터럴로 해석되게 한다.
     */
    public static final char LIKE_ESCAPE = '!';

    private CategoryPath() {
    }

    /** {@code 능력\개발} → {@code ["능력", "개발"]}. 빈 세그먼트는 버린다. */
    public static List<String> segments(String category) {
        if (category == null || category.isBlank()) {
            return List.of();
        }
        return Arrays.stream(category.strip().split("\\\\"))
                .map(String::strip)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /** 앞뒤 공백과 중복 구분자를 정리한 정규 경로. 유효하지 않으면 null. */
    public static String normalize(String category) {
        List<String> segments = segments(category);
        return segments.isEmpty() ? null : String.join(SEPARATOR, segments);
    }

    /** {@code 능력\개발\SpringBoot} → {@code 능력\개발}. 최상위면 null. */
    public static String parentOf(String path) {
        List<String> segments = segments(path);
        if (segments.size() <= 1) {
            return null;
        }
        return String.join(SEPARATOR, segments.subList(0, segments.size() - 1));
    }

    /** 마지막 세그먼트. {@code 능력\개발} → {@code 개발} */
    public static String nameOf(String path) {
        List<String> segments = segments(path);
        return segments.isEmpty() ? null : segments.get(segments.size() - 1);
    }

    /** 하위 카테고리까지 포함해 조회하기 위한 LIKE 패턴. {@code 능력} → {@code 능력\%} */
    public static String descendantPattern(String category) {
        return escapeLike(category.strip()) + SEPARATOR + "%";
    }

    /**
     * 사용자 입력에 LIKE 와일드카드가 들어와도 리터럴로 취급되게 한다.
     * 이스케이프 문자 자신을 가장 먼저 처리해야 이중 치환이 생기지 않는다.
     */
    private static String escapeLike(String value) {
        return value
                .replace(String.valueOf(LIKE_ESCAPE), LIKE_ESCAPE + "" + LIKE_ESCAPE)
                .replace("%", LIKE_ESCAPE + "%")
                .replace("_", LIKE_ESCAPE + "_");
    }
}
