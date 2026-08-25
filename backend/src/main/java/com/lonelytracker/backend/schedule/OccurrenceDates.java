package com.lonelytracker.backend.schedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 반복 규칙에서 회차 날짜를 뽑는다. DB 를 타지 않는 순수 계산이다.
 * <p>
 * 행을 미리 만드는 방식이라 끝이 있어야 한다. 종료일을 주지 않으면 1년치까지만 만든다.
 */
public final class OccurrenceDates {

    /**
     * 한 번에 만들 수 있는 회차 수 상한.
     * 매일 * 1년이 365건이므로 넉넉하다. 넘으면 잘라내지 않고 거부한다.
     */
    public static final int MAX_OCCURRENCES = 500;

    private static final int DEFAULT_YEARS = 1;

    private OccurrenceDates() {
    }

    public static List<LocalDate> generate(RecurrenceFreq freq, Set<DayOfWeek> byWeekday,
                                           LocalDate startsOn, LocalDate endsOn) {
        if (freq == RecurrenceFreq.WEEKLY && (byWeekday == null || byWeekday.isEmpty())) {
            throw new IllegalArgumentException("매주 반복은 요일을 하나 이상 골라야 합니다");
        }

        LocalDate last = (endsOn != null) ? endsOn : startsOn.plusYears(DEFAULT_YEARS);
        if (last.isBefore(startsOn)) {
            throw new IllegalArgumentException("종료일은 시작일보다 이를 수 없습니다");
        }

        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate date = startsOn; !date.isAfter(last); date = date.plusDays(1)) {
            if (freq == RecurrenceFreq.DAILY || byWeekday.contains(date.getDayOfWeek())) {
                dates.add(date);
                if (dates.size() > MAX_OCCURRENCES) {
                    // 조용히 잘라내면 사용자는 2년치를 만들었다고 믿는데 실제로는 일부만 있게 된다
                    throw new IllegalArgumentException(
                            "생성할 일정이 " + MAX_OCCURRENCES + "건을 넘습니다. 종료일을 줄여주세요");
                }
            }
        }
        return dates;
    }
}
