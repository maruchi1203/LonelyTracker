package com.lonelytracker.backend.schedule.domain;

/**
 * 일정을 지울 때의 범위.
 * <p>
 * 기본값은 FUTURE 다. 빠뜨렸을 때 과거 기록까지 날아가면 안 된다.
 * "이 회차만 삭제" 는 두지 않는다 — 습관 추적에서 "이날은 안 함" 은 곧 건너뛰기이므로
 * SKIPPED 로 통일했다.
 */
public enum DeleteScope {
    /** 그만두기. 규칙의 종료일을 오늘로 당겨 이후 회차를 끊고 지난 기록은 남긴다 */
    FUTURE,
    /** 전체 삭제. 회차 기록까지 사라진다 */
    ALL
}
