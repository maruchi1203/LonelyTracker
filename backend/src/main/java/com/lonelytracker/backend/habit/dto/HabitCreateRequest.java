package com.lonelytracker.backend.habit.dto;

import com.lonelytracker.backend.common.FieldLengths;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param categoryId      묶일 갈래. 사용자가 만든 것 중 하나라 값으로 받지 않고 id 로 받는다
 * @param twoMinuteAction 시작에 필요한 2분 이내의 행동. 없어도 된다
 * @param atTime          언제 할지. 시각이든 "퇴근 후" 같은 상황이든 글자 그대로 받는다
 * @param place           어디서 할지
 */
public record HabitCreateRequest(
        @NotBlank(message = "title은 필수입니다") @Size(max = FieldLengths.TITLE) String title,

        @NotNull(message = "categoryId는 필수입니다") Long categoryId,

        @Size(max = FieldLengths.TWO_MINUTE_ACTION) String twoMinuteAction,

        @Size(max = FieldLengths.HABIT_AT_TIME) String atTime,

        @Size(max = FieldLengths.PLACE) String place) {
}
