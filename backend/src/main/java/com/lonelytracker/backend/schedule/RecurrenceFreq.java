package com.lonelytracker.backend.schedule;

/**
 * 반복 주기. 자기개발 습관에 실제로 쓰이는 두 가지만 둔다.
 * 매월·매년·N일 간격은 의도적으로 제외했다 (설계 문서 9절).
 */
public enum RecurrenceFreq {
    /** 매일. byWeekday 를 무시한다 */
    DAILY,
    /** 매주 지정한 요일. byWeekday 가 비어 있으면 안 된다 */
    WEEKLY
}
