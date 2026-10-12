package com.lonelytracker.backend.schedule.domain;

import com.lonelytracker.backend.schedule.entity.ScheduleRecurEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 이 한 줄이 하위 일정의 운명을 가른다 — 회차가 남았으면 지난 회차에 끝낸 일을
 * 그대로 두고, 마지막이었으면 그 일도 함께 끝낸다.
 *
 * <p>DB 를 타지 않는 순수 로직이라 날짜 경계를 여기서 촘촘히 본다.
 */
class HasOccurrenceAfterTest {

    // 2026-10-05 는 월요일이다
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 5);

    @Test
    @DisplayName("1회성 일정에는 남은 회차가 없다")
    void oneOffHasNothingLeft() {
        assertThat(ScheduleUtil.hasOccurrenceAfter(null, MONDAY)).isFalse();
    }

    // 끝까지 펼쳐 볼 수는 없지만 볼 필요도 없다
    @Test
    @DisplayName("종료일이 없으면 언제든 남았다")
    void openEndedAlwaysHasMore() {
        assertThat(ScheduleUtil.hasOccurrenceAfter(
                daily(null), MONDAY.plusYears(10))).isTrue();
    }

    @Test
    @DisplayName("종료일 앞이면 남았다")
    void beforeTheEndHasMore() {
        assertThat(ScheduleUtil.hasOccurrenceAfter(
                daily(MONDAY.plusDays(1)), MONDAY)).isTrue();
    }

    /*
     * 경계다. 마지막 회차를 끝내는 순간이 바로 하위 일정을 함께 끝낼 때라,
     * 여기서 하루만 어긋나면 반복이 끝나도 자손이 영원히 남는다
     */
    @Test
    @DisplayName("종료일 그날이면 더 남지 않았다")
    void theLastDayHasNoMore() {
        assertThat(ScheduleUtil.hasOccurrenceAfter(daily(MONDAY), MONDAY)).isFalse();
    }

    @Test
    @DisplayName("종료일을 지났으면 남지 않았다")
    void afterTheEndHasNoMore() {
        assertThat(ScheduleUtil.hasOccurrenceAfter(
                daily(MONDAY), MONDAY.plusDays(3))).isFalse();
    }

    /*
     * 날짜가 아니라 회차를 본다. 종료일이 뒤에 있어도 그 사이에 해당 요일이 없으면
     * 남은 회차가 없다 — 매주 월요일인데 종료일이 수요일인 경우다
     */
    @Test
    @DisplayName("종료일 전이어도 그 사이에 회차가 없으면 남지 않았다")
    void noMoreWhenNoWeekdayFalls() {
        ScheduleRecurEntity mondays = ScheduleRecurEntity.builder()
                .freq(ScheduleRecurrenceFreq.WEEKLY)
                .byWeekday(EnumSet.of(DayOfWeek.MONDAY))
                .endsOn(MONDAY.plusDays(2))
                .build();

        assertThat(ScheduleUtil.hasOccurrenceAfter(mondays, MONDAY)).isFalse();
    }

    @Test
    @DisplayName("다음 주 같은 요일이 종료일 안이면 남았다")
    void moreWhenNextWeekFits() {
        ScheduleRecurEntity mondays = ScheduleRecurEntity.builder()
                .freq(ScheduleRecurrenceFreq.WEEKLY)
                .byWeekday(EnumSet.of(DayOfWeek.MONDAY))
                .endsOn(MONDAY.plusDays(7))
                .build();

        assertThat(ScheduleUtil.hasOccurrenceAfter(mondays, MONDAY)).isTrue();
    }

    private static ScheduleRecurEntity daily(LocalDate endsOn) {
        return ScheduleRecurEntity.builder()
                .freq(ScheduleRecurrenceFreq.DAILY)
                .byWeekday(Set.of())
                .endsOn(endsOn)
                .build();
    }
}
