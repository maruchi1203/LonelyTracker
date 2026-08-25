package com.lonelytracker.backend.schedule.dto;

import com.lonelytracker.backend.common.FieldLengths;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

/**
 * 앞으로 전부 수정. 템플릿과 규칙을 함께 바꾼다.
 * <p>
 * 생성 요청과 달리 {@code startAt} 이 아니라 {@code startTime} 을 받는다.
 * 날짜는 규칙이 정하므로 시리즈가 아는 것은 시각뿐이기 때문이다.
 * <p>
 * override 가 붙어 있는 회차는 이 수정에 영향받지 않는다. 이미 손댄 회차이므로
 * 그쪽 값이 이긴다.
 */
public record ScheduleSeriesUpdateRequest(
        @NotBlank(message = "title은 필수입니다")
        @Size(max = FieldLengths.TITLE, message = "title은 200자를 넘을 수 없습니다")
        String title,

        @Size(max = FieldLengths.DESCRIPTION, message = "description은 20000자를 넘을 수 없습니다")
        String description,

        @NotNull(message = "startTime은 필수입니다")
        LocalTime startTime,

        /** null 이면 종료 시각이 없는 일정이다 */
        Integer durationMinutes,

        Boolean allDay,

        @Size(max = FieldLengths.CATEGORY_NAME, message = "category는 50자를 넘을 수 없습니다")
        String category,

        @NotNull(message = "recurrence는 필수입니다")
        @Valid
        RecurrenceRequest recurrence
) {
}
