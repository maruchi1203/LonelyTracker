package com.lonelytracker.backend.schedule.domain;

/**
 * 일정을 지울 때의 범위
 * 테스트에서 scope 검증을 위한 로직이 있어 해당 과정에서 사용
 */
public enum ScheduleDeleteScope {
    FUTURE,
    ALL
}
