package com.lonelytracker.backend.schedule.domain;

/**
 * MoSCoW 우선순위.
 * 값이 없으면 {@link #COULD} 로 본다. 기본값을 두지 않아 "아직 안 정함"과 구분된다.
 */
public enum SchedulePriority {

    /** 반드시 해야 한다 */
    MUST,

    /** 하는 편이 좋다 */
    SHOULD,

    /** 여유가 되면 한다 */
    COULD,

    /**
     * 안 하기로 했다.
     * 지우지 않고 남겨 판단을 기록한다. 리스트에는 흐리게 남고 달력에서는 빠진다
     */
    WONT
}
