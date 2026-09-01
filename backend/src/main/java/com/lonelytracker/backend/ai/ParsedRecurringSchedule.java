package com.lonelytracker.backend.ai;

import com.lonelytracker.backend.schedule.domain.ScheduleRecurrenceFreq;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/**
 * AI에게 받은 텍스트 기반해 반복 일정으로 파싱용
 */
public record ParsedRecurringSchedule(
        ScheduleRecurrenceFreq freq,
        Set<DayOfWeek> byWeekday,
        LocalDate endsOn) {
}
