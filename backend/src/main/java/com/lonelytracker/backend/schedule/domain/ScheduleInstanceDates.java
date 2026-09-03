package com.lonelytracker.backend.schedule.domain;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 반복 규칙에서 회차 날짜를 뽑는다.
 */
public final class ScheduleInstanceDates {

    /** 한 번에 펼칠 수 있는 회차 수 상한. 조회 범위가 너무 넓을 때를 막는다 */
    public static final int MAX_EXPANDED = 10_000;

    private ScheduleInstanceDates() {
    }

    /**
     * 규칙에 맞는 날짜를 돌려준다. 양끝을 포함한다.
     *
     * @param freq      반복 주기 {@link ScheduleRecurrenceFreq}
     * @param byWeekday 반복 요일. WEEKLY에서만 쓰인다
     * @param from      구간 시작일
     * @param to        구간 종료일
     * @throws IllegalArgumentException 구간이 뒤집혔거나 결과가 {@link #MAX_EXPANDED} 를 넘을 때
     */
    public static List<LocalDate> generate(ScheduleRecurrenceFreq freq, Set<DayOfWeek> byWeekday,
            LocalDate from, LocalDate to) {
        if (freq == ScheduleRecurrenceFreq.WEEKLY && (byWeekday == null || byWeekday.isEmpty())) {
            throw new IllegalArgumentException("매주 반복은 요일을 하나 이상 골라야 합니다");
        }
        if (from == null || to == null) {
            throw new IllegalArgumentException("전개 구간의 시작과 끝이 모두 있어야 합니다");
        }
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("종료일은 시작일보다 이를 수 없습니다");
        }

        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            if (freq == ScheduleRecurrenceFreq.DAILY || byWeekday.contains(date.getDayOfWeek())) {
                dates.add(date);
                if (dates.size() > MAX_EXPANDED) {
                    throw new IllegalArgumentException(
                            "조회 범위가 너무 넓습니다. 회차가 " + MAX_EXPANDED + "건을 넘습니다");
                }
            }
        }
        return dates;
    }
}
