package com.lonelytracker.backend.habit.dto;

import jakarta.validation.constraints.NotNull;

/** 준 것으로 통째로 덮어쓴다 */
public record HabitUpdateRequest(
        String title,

        /*
         * 갈래는 비울 수 없다. 안 받고 넘기면 NOT NULL 에 걸려 500 이 나가므로
         * 여기서 막아 어디가 모자란지 말로 알린다
         */
        @NotNull(message = "categoryId는 필수입니다") Long categoryId,
        String twoMinuteAction,
        String atTime,
        String place) {
}
