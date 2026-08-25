package com.lonelytracker.backend.schedule.dto;

import com.lonelytracker.backend.common.FieldLengths;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 첫 회차의 내용 + 반복 규칙.
 * <p>
 * startAt 의 <b>날짜</b>는 시리즈 시작일이 되고 <b>시각</b>은 템플릿의 startTime 이 된다.
 * endAt 이 있으면 소요시간(durationMinutes)으로 환산해 담는다.
 */
public record ScheduleSeriesCreateRequest(
        @NotBlank(message = "title은 필수입니다")
        @Size(max = FieldLengths.TITLE, message = "title은 200자를 넘을 수 없습니다")
        String title,

        @Size(max = FieldLengths.DESCRIPTION, message = "description은 20000자를 넘을 수 없습니다")
        String description,

        @NotNull(message = "startAt은 필수입니다")
        LocalDateTime startAt,

        LocalDateTime endAt,

        Boolean allDay,

        @Size(max = FieldLengths.CATEGORY_NAME, message = "category는 50자를 넘을 수 없습니다")
        String category,

        @NotNull(message = "recurrence는 필수입니다")
        @Valid
        RecurrenceRequest recurrence
) {
}
