package com.lonelytracker.backend.schedule.dto;

import com.lonelytracker.backend.common.FieldLengths;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 일정 수정 = <b>앞으로 전부 수정</b>.
 * <p>
 * 회차 기록(schedule_progress)이 붙어 있는 회차는 영향받지 않는다.
 * 이미 손댄 회차이므로 그쪽 값이 이긴다.
 *
 * @param recurrence 주면 반복 규칙을 바꾸거나 새로 붙이고, 안 주면 <b>반복을 없앤다</b>
 */
public record ScheduleUpdateRequest(
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

        @Valid
        RecurrenceRequest recurrence
) {
}
