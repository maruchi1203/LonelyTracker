package com.lonelytracker.backend.schedule.dto;

/**
 * 반복 생성 결과.
 * <p>
 * 회차를 미리 만들지 않으므로 "몇 건 만들어졌는지" 라는 개념 자체가 없다.
 * 대신 첫 회차를 돌려줘 화면이 바로 보여줄 수 있게 한다.
 */
public record ScheduleSeriesResponse(
        Long seriesId,
        ScheduleResponse firstOccurrence
) {
}
