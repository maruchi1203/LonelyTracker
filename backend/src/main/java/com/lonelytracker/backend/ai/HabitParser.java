package com.lonelytracker.backend.ai;

import java.util.List;

/**
 * 자연어를 습관 초안으로 바꾼다.
 *
 * <p>일정 파서와 계약을 나눠 둔다. 받아야 하는 것이 다르고(카테고리·2분 행동),
 * 물어야 하는 것도 다르다(시각이 아니라 신호). 한 계약에 담으면 한쪽 칸이 늘 비어 온다.
 */
public interface HabitParser {

    /** 문장 하나에 습관이 여럿 들어 있을 수 있다. 그 호출이 쓴 토큰도 함께 온다 */
    HabitParseResult parse(HabitParseCommand command);

    /**
     * 습관 파싱 한 번에 필요한 것 전부.
     *
     * @param text       사용자가 친 문장
     * @param categories 고를 수 있는 카테고리 이름. 스키마의 enum 으로 박아 이 밖을 못 고르게 한다
     * @param baseUrl    제공자 주소. 규약도 이 값으로 고른다
     */
    record HabitParseCommand(
            String text,
            List<String> categories,
            String baseUrl,
            String model,
            String apiKey) {
    }

    /** @param habits 문장에서 읽어낸 초안들. 하나뿐이면 길이 1의 목록이다 */
    record HabitParseResult(List<ParsedHabit> habits, AiUsage usage) {
    }
}
