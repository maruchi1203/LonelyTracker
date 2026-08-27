package com.lonelytracker.backend.ai;

import com.lonelytracker.backend.schedule.RecurrenceFreq;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/**
 * 파싱된 반복 규칙. null 이면 1회성 일정이다.
 * <p>
 * {@code RecurrenceRequest} 와 같은 모양이라 화면이 그대로 되쏠 수 있다.
 */
public record ParsedRecurrence(
        RecurrenceFreq freq,
        Set<DayOfWeek> byWeekday,
        LocalDate endsOn
) {
}
