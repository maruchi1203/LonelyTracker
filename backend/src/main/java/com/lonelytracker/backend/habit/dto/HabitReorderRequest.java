package com.lonelytracker.backend.habit.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 한 카테고리 안의 습관 차례를 다시 세운다.
 * 받은 차례대로 0부터 번호를 다시 매긴다.
 *
 * @param categoryId 그 무리가 선 카테고리
 * @param ids        그 카테고리의 습관 전부. 카테고리를 옮기는 일은 습관 수정이 맡는다
 */
public record HabitReorderRequest(
        @NotNull(message = "categoryId는 비울 수 없습니다") Long categoryId,

        @NotEmpty(message = "ids는 비울 수 없습니다") List<Long> ids) {
}
