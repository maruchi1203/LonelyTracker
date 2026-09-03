package com.lonelytracker.backend.schedule.dto;

import com.lonelytracker.backend.schedule.domain.ScheduleRecurrenceFreq;
import com.lonelytracker.backend.schedule.entity.ScheduleRecurEntity;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/**
 * 반복 규칙. 수정 폼이 지금 규칙을 읽어야 하므로 요청과 같은 모양으로 돌려준다.
 *
 * @param byWeekday WEEKLY 일 때만 채워진다
 * @param endsOn    null이면 무기한
 */
public record ScheduleRecurrenceResponse(
        ScheduleRecurrenceFreq freq,
        Set<DayOfWeek> byWeekday,
        LocalDate endsOn) {

    public static ScheduleRecurrenceResponse from(ScheduleRecurEntity recur) {
        return new ScheduleRecurrenceResponse(
                recur.getFreq(), recur.getByWeekday(), recur.getEndsOn());
    }
}
