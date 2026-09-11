package com.lonelytracker.backend.habit.dto;

import com.lonelytracker.backend.common.FieldLengths;
import com.lonelytracker.backend.habit.domain.HabitCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param twoMinuteAction 시작에 필요한 2분 이내의 행동. 없어도 된다
 */
public record HabitCreateRequest(
        @NotBlank(message = "title은 필수입니다") @Size(max = FieldLengths.TITLE) String title,

        @NotNull(message = "category는 필수입니다") HabitCategory category,

        @Size(max = FieldLengths.TWO_MINUTE_ACTION) String twoMinuteAction) {
}
