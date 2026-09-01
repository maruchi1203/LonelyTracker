package com.lonelytracker.backend.schedule.domain;

/**
 * 일정 수행 상태. 주간 수행률 리포트의 계산 근거가 된다.
 * <p>
 * 연기는 상태가 아니다. override 행이 원래 날짜(onDate)와 옮긴 시각(startAt)을
 * 함께 갖고 postponeCount 로 횟수를 세므로, 별도 상태가 필요 없다.
 */
public enum ScheduleStatus {
    PLANNED,
    DONE,
    /** 안 하기로 확정. 회차가 여기서 끝난다 */
    SKIPPED
}
