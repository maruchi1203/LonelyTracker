package com.lonelytracker.backend.habit.dto;

import com.lonelytracker.backend.common.FieldLengths;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 그날 해냈는지 표시한다.
 *
 * @param done false 면 그날 기록을 지운다
 * @param note 그날 남길 한 줄. 없어도 된다
 */
public record HabitLogRequest(
        @NotNull(message = "done은 필수입니다") Boolean done,

        @Size(max = FieldLengths.TWO_MINUTE_ACTION) String note) {
}
