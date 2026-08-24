package com.lonelytracker.backend.schedule;

/** 일정 수행 상태. 주간 수행률 리포트의 계산 근거가 된다. */
public enum ScheduleStatus {
    PLANNED,
    DONE,
    /** 안 하기로 확정. 회차가 여기서 끝난다 */
    SKIPPED,
    /**
     * 다른 날로 미뤘고, 그 날짜에 새 회차가 있다. 아직 끝나지 않은 일이다.
     * SKIPPED 와 합치면 "미룬 사람"과 "그만둔 사람"이 구분되지 않는다.
     * 이 상태는 /postpone 을 통해서만 붙는다. 상태 변경 API 로는 붙일 수 없다.
     */
    POSTPONED
}
