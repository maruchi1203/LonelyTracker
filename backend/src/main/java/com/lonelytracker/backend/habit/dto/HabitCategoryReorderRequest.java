package com.lonelytracker.backend.habit.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 카테고리 차례를 통째로 다시 세운다.
 * 받은 차례대로 0부터 번호를 다시 매긴다.
 *
 * @param ids 그 사람의 카테고리 전부. 하나라도 빠지면 거절한다
 */
public record HabitCategoryReorderRequest(
        @NotEmpty(message = "ids는 비울 수 없습니다") List<Long> ids) {
}
