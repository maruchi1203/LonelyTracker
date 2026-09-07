package com.lonelytracker.backend.schedule.dto;

/**
 * 최근 몇 주의 성적
 * 날짜 기준이라 시각은 보지 않음
 *
 * @param weeks   집계한 주 수
 * @param passed  날짜가 지난 회차 수. 수행률의 분모다
 * @param done    그중 완료한 수. 수행률의 분자다
 * @param skipped 그중 건너뛴 수. 분모에는 남는다
 * @param moved   그중 원래 날짜에 하지 않은 수
 * @param early   아직 오지 않은 날인데 완료한 수. 분모 밖이다
 */
public record ScheduleStatsResponse(
                int weeks,
                int passed,
                int done,
                int skipped,
                int moved,
                int early) {
}
