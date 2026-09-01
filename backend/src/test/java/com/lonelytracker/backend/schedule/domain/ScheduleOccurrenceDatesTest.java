package com.lonelytracker.backend.schedule.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 회차 날짜 계산. DB 를 타지 않는 순수 로직이라 단위 테스트로 둔다.
 * 통합 테스트는 컨테이너를 띄우느라 느리므로, 날짜 경계는 여기서 촘촘히 본다.
 */
class ScheduleOccurrenceDatesTest {

    // 2026-08-24 는 월요일이다
    private static final LocalDate MONDAY = LocalDate.of(2026, 8, 24);

    @Test
    @DisplayName("매주 월수금이면 그 요일에만 생긴다")
    void weeklyPicksOnlyGivenWeekdays() {
        List<LocalDate> dates = ScheduleOccurrenceDates.generate(
                ScheduleRecurrenceFreq.WEEKLY,
                EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                MONDAY,
                MONDAY.plusDays(6));

        assertThat(dates).containsExactly(
                LocalDate.of(2026, 8, 24),
                LocalDate.of(2026, 8, 26),
                LocalDate.of(2026, 8, 28));
    }

    @Test
    @DisplayName("매일이면 요일 지정을 무시하고 모든 날에 생긴다")
    void dailyIgnoresWeekdays() {
        List<LocalDate> dates = ScheduleOccurrenceDates.generate(
                ScheduleRecurrenceFreq.DAILY,
                EnumSet.of(DayOfWeek.MONDAY),
                MONDAY,
                MONDAY.plusDays(4));

        assertThat(dates).hasSize(5);
    }

    @Test
    @DisplayName("시작일이 지정 요일이 아니어도 첫 회차는 다음 해당 요일에 생긴다")
    void startsAtFirstMatchingWeekday() {
        List<LocalDate> dates = ScheduleOccurrenceDates.generate(
                ScheduleRecurrenceFreq.WEEKLY,
                EnumSet.of(DayOfWeek.THURSDAY),
                MONDAY,
                MONDAY.plusDays(6));

        assertThat(dates).containsExactly(LocalDate.of(2026, 8, 27));
    }

    @Test
    @DisplayName("구간을 주지 않으면 거부한다")
    void requiresExplicitRange() {
        // 기본값을 두면 호출자가 범위를 안 정해도 통과해 버린다.
        // 무기한 반복은 시리즈의 endsOn 이 null 인 것으로 표현하고,
        // 조회 범위로 잘라서 이 함수에 넘긴다.
        assertThatThrownBy(() -> ScheduleOccurrenceDates.generate(
                ScheduleRecurrenceFreq.DAILY, EnumSet.noneOf(DayOfWeek.class), MONDAY, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("1년을 넘는 구간도 그대로 펼친다")
    void expandsBeyondOneYear() {
        // 회차를 미리 만들지 않으므로 기간에 인위적 상한이 없다.
        // 달력에 연간 보기를 붙여도 동작해야 한다.
        List<LocalDate> dates = ScheduleOccurrenceDates.generate(
                ScheduleRecurrenceFreq.DAILY, EnumSet.noneOf(DayOfWeek.class),
                MONDAY, MONDAY.plusYears(2));

        // 2026-08-24 ~ 2028-08-24 양끝 포함. 2028 이 윤년이라 732 다
        assertThat(dates).hasSize(732);
    }

    @Test
    @DisplayName("종료일 당일까지 포함한다")
    void endDateIsInclusive() {
        List<LocalDate> dates = ScheduleOccurrenceDates.generate(
                ScheduleRecurrenceFreq.DAILY, EnumSet.noneOf(DayOfWeek.class), MONDAY, MONDAY);

        assertThat(dates).containsExactly(MONDAY);
    }

    @Test
    @DisplayName("터무니없이 넓은 구간은 잘라내지 않고 거부한다")
    void rejectsAbsurdRange() {
        // 조용히 잘라내면 호출자는 100년치를 받았다고 믿는데 실제로는 일부만 온다.
        assertThatThrownBy(() -> ScheduleOccurrenceDates.generate(
                ScheduleRecurrenceFreq.DAILY, EnumSet.noneOf(DayOfWeek.class),
                MONDAY, MONDAY.plusYears(100)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.valueOf(ScheduleOccurrenceDates.MAX_EXPANDED));
    }

    @Test
    @DisplayName("매주인데 요일을 하나도 안 고르면 거부한다")
    void weeklyRequiresWeekday() {
        assertThatThrownBy(() -> ScheduleOccurrenceDates.generate(
                ScheduleRecurrenceFreq.WEEKLY, EnumSet.noneOf(DayOfWeek.class), MONDAY, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("종료일이 시작일보다 이르면 거부한다")
    void rejectsInvertedRange() {
        assertThatThrownBy(() -> ScheduleOccurrenceDates.generate(
                ScheduleRecurrenceFreq.DAILY, EnumSet.noneOf(DayOfWeek.class),
                MONDAY, MONDAY.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
