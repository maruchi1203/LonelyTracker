package com.lonelytracker.backend.habit.dto;

import java.util.List;

/**
 * 파싱 결과.
 *
 * @param habits 읽어낸 초안들. 쓸 수 없는 것은 버리고 남은 것만 온다
 * @param notice 초안이 없는 이유. 오류가 아니라 안내다. 초안이 있으면 null
 */
public record HabitParseResponse(List<HabitDraftResponse> habits, String notice) {

    public static HabitParseResponse of(List<HabitDraftResponse> habits) {
        return new HabitParseResponse(habits, null);
    }

    public static HabitParseResponse notice(String notice) {
        return new HabitParseResponse(List.of(), notice);
    }
}
