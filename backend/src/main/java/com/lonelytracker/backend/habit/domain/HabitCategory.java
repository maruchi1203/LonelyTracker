package com.lonelytracker.backend.habit.domain;

/**
 * 습관이 기르는 갈래.
 * 여섯 개를 고정으로 둔다. 화면이 "각 갈래에 하나씩"을 세려면 아는 목록이어야 한다.
 */
public enum HabitCategory {
    /** 운동 */
    BODY,
    /** 마음챙김 */
    MIND,
    /** 부업 */
    SIDE_JOB,
    /** 예술 */
    ART,
    /** 학습 */
    LEARNING,
    /** 인간관계 */
    RELATIONSHIP
}
