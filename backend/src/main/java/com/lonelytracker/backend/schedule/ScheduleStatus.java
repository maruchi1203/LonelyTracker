package com.lonelytracker.backend.schedule;

/** 일정 수행 상태. 7일차 주간 수행률 리포트의 계산 근거가 된다. */
public enum ScheduleStatus {
    PLANNED,
    DONE,
    SKIPPED
}
