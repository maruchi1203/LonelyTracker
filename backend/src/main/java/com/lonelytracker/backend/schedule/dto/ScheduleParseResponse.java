package com.lonelytracker.backend.schedule.dto;

import com.lonelytracker.backend.ai.ParsedSchedule;

import java.util.List;

/**
 * 파싱 결과
 *
 * @param schedules 읽어낸 초안들. 쓸 수 없는 것은 버리고 남은 것만 온다
 * @param notice    초안이 없는 이유. 오류가 아니라 안내다. 초안이 있으면 null
 */
public record ScheduleParseResponse(List<ParsedSchedule> schedules, String notice) {

    public static ScheduleParseResponse of(List<ParsedSchedule> schedules) {
        return new ScheduleParseResponse(schedules, null);
    }

    public static ScheduleParseResponse notice(String notice) {
        return new ScheduleParseResponse(List.of(), notice);
    }
}
