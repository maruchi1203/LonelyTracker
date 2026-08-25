package com.lonelytracker.backend.schedule;

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
class OccurrenceDatesTest {

    // 2026-08-24 는 월요일이다
    private static final LocalDate MONDAY = LocalDate.of(2026, 8, 24);

    @Test
    @DisplayName("매주 월수금이면 그 요일에만 생긴다")
    void weeklyPicksOnlyGivenWeekdays() {
        List<LocalDate> dates = OccurrenceDates.generate(
                RecurrenceFreq.WEEKLY,
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
        List<LocalDate> dates = OccurrenceDates.generate(
                RecurrenceFreq.DAILY,
                EnumSet.of(DayOfWeek.MONDAY),
                MONDAY,
                MONDAY.plusDays(4));

        assertThat(dates).hasSize(5);
    }

    @Test
    @DisplayName("시작일이 지정 요일이 아니어도 첫 회차는 다음 해당 요일에 생긴다")
    void startsAtFirstMatchingWeekday() {
        List<LocalDate> dates = OccurrenceDates.generate(
                RecurrenceFreq.WEEKLY,
                EnumSet.of(DayOfWeek.THURSDAY),
                MONDAY,
                MONDAY.plusDays(6));

        assertThat(dates).containsExactly(LocalDate.of(2026, 8, 27));
    }

    @Test
    @DisplayName("종료일을 주지 않으면 1년치를 만든다")
    void defaultsToOneYear() {
        List<LocalDate> dates = OccurrenceDates.generate(
                RecurrenceFreq.WEEKLY,
                EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY),
                MONDAY,
                null);

        assertThat(dates).hasSizeBetween(150, 160);
        assertThat(dates.get(dates.size() - 1)).isBeforeOrEqualTo(MONDAY.plusYears(1));
    }

    @Test
    @DisplayName("종료일 당일까지 포함한다")
    void endDateIsInclusive() {
        List<LocalDate> dates = OccurrenceDates.generate(
                RecurrenceFreq.DAILY, EnumSet.noneOf(DayOfWeek.class), MONDAY, MONDAY);

        assertThat(dates).containsExactly(MONDAY);
    }

    @Test
    @DisplayName("500건을 넘으면 잘라내지 않고 거부한다")
    void rejectsWhenOverLimit() {
        assertThatThrownBy(() -> OccurrenceDates.generate(
                RecurrenceFreq.DAILY, EnumSet.noneOf(DayOfWeek.class),
                MONDAY, MONDAY.plusYears(2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("500");
    }

    @Test
    @DisplayName("매주인데 요일을 하나도 안 고르면 거부한다")
    void weeklyRequiresWeekday() {
        assertThatThrownBy(() -> OccurrenceDates.generate(
                RecurrenceFreq.WEEKLY, EnumSet.noneOf(DayOfWeek.class), MONDAY, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("종료일이 시작일보다 이르면 거부한다")
    void rejectsInvertedRange() {
        assertThatThrownBy(() -> OccurrenceDates.generate(
                RecurrenceFreq.DAILY, EnumSet.noneOf(DayOfWeek.class),
                MONDAY, MONDAY.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
