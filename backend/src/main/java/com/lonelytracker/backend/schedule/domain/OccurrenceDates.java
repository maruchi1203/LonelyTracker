package com.lonelytracker.backend.schedule.domain;

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
     * 한 번에 펼칠 수 있는 회차 수 상한.
     * <p>
     * 생성 제한이 아니라 <b>조회 폭에 대한 안전장치</b>다. 회차를 미리 만들지 않으므로
     * 아무리 긴 반복도 저장에는 문제가 없지만, 사용자가 준 범위를 그대로 믿고 펼치면
     * 100년치 요청 하나로 메모리를 태울 수 있다.
     * <p>
     * 매일 반복 기준 약 27년이라 화면에서 나올 수 있는 요청은 전부 통과한다.
     */
    public static final int MAX_EXPANDED = 10_000;

    private OccurrenceDates() {
    }

    /**
     * {@code [from, to]} 구간에서 규칙에 맞는 날짜를 돌려준다. 양끝을 포함한다.
     * <p>
     * <b>구간은 부르는 쪽이 정한다.</b> 기본값을 두지 않는 이유는, 기본값이 있으면
     * 호출자가 범위를 안 정해도 통과해 버리기 때문이다. 무기한 반복은 시리즈의
     * {@code endsOn} 이 null 인 것으로 표현하고, 조회 범위로 잘라서 이 함수에 넘긴다.
     *
     * @throws IllegalArgumentException 구간이 뒤집혔거나 결과가 {@link #MAX_EXPANDED} 를 넘을 때
     */
    public static List<LocalDate> generate(RecurrenceFreq freq, Set<DayOfWeek> byWeekday,
            LocalDate from, LocalDate to) {
        if (freq == RecurrenceFreq.WEEKLY && (byWeekday == null || byWeekday.isEmpty())) {
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
            if (freq == RecurrenceFreq.DAILY || byWeekday.contains(date.getDayOfWeek())) {
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
