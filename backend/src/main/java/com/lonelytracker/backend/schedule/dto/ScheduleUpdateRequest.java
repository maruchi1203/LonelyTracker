package com.lonelytracker.backend.schedule.dto;

import com.lonelytracker.backend.common.FieldLengths;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 앞으로의 회차를 전부 수정한다. 이미 손댄 회차는 그쪽 값이 이겨 영향받지 않는다.
 *
 * @param recurrence 주면 반복 규칙을 바꾸거나 새로 붙이고, 안 주면 반복을 없앤다
 */
public record ScheduleUpdateRequest(
                @NotBlank(message = "title은 필수입니다") @Size(max = FieldLengths.TITLE, message = "title은 200자를 넘을 수 없습니다") String title,

                @Size(max = FieldLengths.DESCRIPTION, message = "description은 20000자를 넘을 수 없습니다") String description,

                LocalDateTime startAt,

                LocalDateTime endAt,

                Boolean allDay,

                Set<@Size(max = FieldLengths.TAG, message = "태그 하나는 50자를 넘을 수 없습니다") String> tags,

                @Size(max = FieldLengths.PLACE, message = "place는 200자를 넘을 수 없습니다") String place,

                @Size(max = FieldLengths.TWO_MINUTE_ACTION, message = "twoMinuteAction은 200자를 넘을 수 없습니다") String twoMinuteAction,

                @Valid ScheduleRecurrenceRequest recurrence) {
}
