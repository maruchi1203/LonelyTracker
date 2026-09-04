package com.lonelytracker.backend.schedule.domain;

import com.lonelytracker.backend.schedule.dto.ScheduleStatsResponse;
import com.lonelytracker.backend.schedule.entity.ScheduleEntity;
import com.lonelytracker.backend.schedule.entity.ScheduleProgressEntity;
import com.lonelytracker.backend.schedule.entity.ScheduleRecurEntity;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 반복 일정의 최근 성적을 센다. DB와 스프링을 모른다.
 * 손 안 댄 회차는 기록이 없으므로 분모는 규칙을 펼쳐서 구한다.
 */
public final class ScheduleStatsCounter {

    private ScheduleStatsCounter() {
    }

    /**
     * 최근 몇 주의 성적. 날짜만 보고 시각은 보지 않는다.
     *
     * @param progresses 그 일정의 회차 기록. 창 밖의 것이 섞여 있어도 된다
     * @param today      오늘 날짜. 호출자가 넘긴다
     * @return 지나간 회차가 없으면 passed 가 0이다
     */
    public static ScheduleStatsResponse count(ScheduleEntity schedule, ScheduleRecurEntity recur,
            List<ScheduleProgressEntity> progresses, LocalDate today, int weeks) {

        Set<LocalDate> passedDates = passedDatesOf(schedule, recur, today, weeks);

        int done = 0;
        int skipped = 0;
        int moved = 0;
        int early = 0;

        for (ScheduleProgressEntity p : progresses) {
            if (passedDates.contains(p.getOnDate())) {
                if (p.getStatus() == ScheduleStatus.DONE) {
                    done++;
                } else if (p.getStatus() == ScheduleStatus.SKIPPED) {
                    skipped++;
                }
                if (p.getStartAt() != null && !p.getStartAt().toLocalDate().equals(p.getOnDate())) {
                    moved++;
                }
            } else if (p.getStatus() == ScheduleStatus.DONE && p.getOnDate().isAfter(today)) {
                early++;
            }
        }

        return new ScheduleStatsResponse(weeks, passedDates.size(), done, skipped, moved, early);
    }

    /** 창 안에서 날짜가 이미 지난 회차들. 오늘 것은 아직 진행 중이라 넣지 않는다 */
    private static Set<LocalDate> passedDatesOf(ScheduleEntity schedule, ScheduleRecurEntity recur,
            LocalDate today, int weeks) {

        LocalDate scheduleStart = schedule.getStartAt().toLocalDate();
        LocalDate windowStart = today.minusWeeks(weeks);
        if (scheduleStart.isAfter(windowStart)) {
            windowStart = scheduleStart;
        }

        LocalDate windowEnd = today.minusDays(1);
        if (recur.getEndsOn() != null && recur.getEndsOn().isBefore(windowEnd)) {
            windowEnd = recur.getEndsOn();
        }
        if (windowEnd.isBefore(windowStart)) {
            return Set.of();
        }

        return new HashSet<>(ScheduleInstanceDates.generate(
                recur.getFreq(), recur.getByWeekday(), windowStart, windowEnd));
    }
}
