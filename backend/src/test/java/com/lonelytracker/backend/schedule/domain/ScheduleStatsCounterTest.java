package com.lonelytracker.backend.schedule.domain;

import com.lonelytracker.backend.schedule.dto.ScheduleStatsResponse;
import com.lonelytracker.backend.schedule.entity.ScheduleEntity;
import com.lonelytracker.backend.schedule.entity.ScheduleProgressEntity;
import com.lonelytracker.backend.schedule.entity.ScheduleRecurEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 성적 세기. DB를 타지 않아 오늘 날짜를 인자로 넣고 경계를 그대로 확인한다.
 */
class ScheduleStatsCounterTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 9, 30);

    @Test
    @DisplayName("분모는 날짜가 지난 회차다. 오늘과 미래는 안 센다")
    void countsOnlyPassedDates() {
        ScheduleStatsResponse s = count(TODAY.minusDays(6), List.of());

        // 9/24 ~ 9/29 여섯 번. 오늘(9/30)은 아직 진행 중이다
        assertThat(s.passed()).isEqualTo(6);
        assertThat(s.done()).isZero();
    }

    @Test
    @DisplayName("건너뛴 회차는 분모에 남고 분자에는 안 들어간다")
    void skippedStaysInTheDenominator() {
        ScheduleStatsResponse s = count(TODAY.minusDays(3), List.of(
                progress(TODAY.minusDays(3), ScheduleStatus.DONE, null),
                progress(TODAY.minusDays(2), ScheduleStatus.SKIPPED, null)));

        assertThat(s.passed()).isEqualTo(3);
        assertThat(s.done()).isEqualTo(1);
        assertThat(s.skipped()).isEqualTo(1);
    }

    @Test
    @DisplayName("아직 오지 않은 날을 완료하면 조기 종료로 따로 센다")
    void completingAFutureDateCountsAsEarly() {
        ScheduleStatsResponse s = count(TODAY.minusDays(2), List.of(
                progress(TODAY.plusDays(1), ScheduleStatus.DONE, null)));

        // 분모 밖이라 100%를 넘을 수 없다
        assertThat(s.passed()).isEqualTo(2);
        assertThat(s.done()).isZero();
        assertThat(s.early()).isEqualTo(1);
    }

    @Test
    @DisplayName("원래 날짜에 하지 않은 회차를 센다")
    void countsMovedInstances() {
        ScheduleStatsResponse s = count(TODAY.minusDays(3), List.of(
                progress(TODAY.minusDays(3), ScheduleStatus.DONE,
                        TODAY.minusDays(2).atTime(9, 0))));

        assertThat(s.done()).isEqualTo(1);
        assertThat(s.moved()).isEqualTo(1);
    }

    @Test
    @DisplayName("일정이 늦게 시작했으면 그날부터만 센다")
    void windowStartsAtTheScheduleStart() {
        assertThat(count(TODAY.minusDays(2), List.of()).passed()).isEqualTo(2);
    }

    @Test
    @DisplayName("반복이 끝났으면 종료일까지만 센다")
    void windowEndsAtTheRuleEnd() {
        ScheduleEntity schedule = schedule(TODAY.minusDays(10));
        ScheduleRecurEntity recur = recur(TODAY.minusDays(7));

        assertThat(ScheduleStatsCounter.count(schedule, recur, List.of(), TODAY, 4).passed())
                .isEqualTo(4);   // 20일 ~ 23일
    }

    @Test
    @DisplayName("셀 회차가 없으면 0이다")
    void noPassedInstances() {
        ScheduleStatsResponse s = count(TODAY.plusDays(5), List.of());

        assertThat(s.passed()).isZero();
    }

    // --- 헬퍼 -------------------------------------------------------------

    private ScheduleStatsResponse count(LocalDate start, List<ScheduleProgressEntity> progresses) {
        return ScheduleStatsCounter.count(schedule(start), recur(null), progresses, TODAY, 4);
    }

    private ScheduleEntity schedule(LocalDate start) {
        return ScheduleEntity.builder().title("운동").startAt(start.atTime(7, 0)).build();
    }

    /** 매일 반복. 요일을 안 봐도 되어 경계 계산만 남는다 */
    private ScheduleRecurEntity recur(LocalDate endsOn) {
        return ScheduleRecurEntity.builder()
                .freq(ScheduleRecurrenceFreq.DAILY)
                .byWeekday(Set.of())
                .endsOn(endsOn)
                .build();
    }

    private ScheduleProgressEntity progress(LocalDate onDate, ScheduleStatus status,
            LocalDateTime startAt) {
        return ScheduleProgressEntity.builder()
                .onDate(onDate)
                .status(status)
                .startAt(startAt)
                .build();
    }
}
