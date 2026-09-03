package com.lonelytracker.backend.ai;

import com.lonelytracker.backend.schedule.domain.ScheduleRecurrenceFreq;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/**
 * 초안의 반복 규칙. 1회성이면 통째로 없다.
 *
 * @param byWeekday WEEKLY 일 때만 쓴다
 * @param endsOn    null이면 무기한
 */
public record ParsedRecurringSchedule(
        ScheduleRecurrenceFreq freq,
        Set<DayOfWeek> byWeekday,
        LocalDate endsOn) {
}
