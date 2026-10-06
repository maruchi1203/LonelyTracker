package com.lonelytracker.backend.habit.dto;

import com.lonelytracker.backend.common.FieldLengths;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 카테고리를 만들 때와 이름을 고칠 때가 같은 모양이다. 가진 값이 이름 하나뿐이다 */
public record HabitCategoryRequest(
        @NotBlank(message = "name은 필수입니다")
        @Size(max = FieldLengths.HABIT_CATEGORY_NAME) String name) {
}
