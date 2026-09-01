package com.lonelytracker.backend.schedule.dto;

import com.lonelytracker.backend.schedule.domain.ScheduleRecurrenceFreq;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

/**
 * @param byWeekday WEEKLY 일 때만 쓴다. DAILY 면 무시된다.
 *                  비어 있는지 검사는 ScheduleOccurrenceDates 가 한다 (freq 를 같이 봐야 하므로)
 * @param endsOn    null 이면 무기한. 회차를 미리 만들지 않으므로 상한이 필요 없다
 */
public record RecurrenceRequest(
                @NotNull(message = "freq는 필수입니다") ScheduleRecurrenceFreq freq,

                Set<DayOfWeek> byWeekday,

                LocalDate endsOn) {
}
