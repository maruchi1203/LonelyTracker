package com.lonelytracker.backend.schedule.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 형제 무리의 순서를 통째로 다시 정한다.
 * 받은 차례대로 0부터 번호를 다시 매긴다.
 *
 * @param parentId null이면 최상위 무리다
 * @param ids      그 무리에 속한 일정 전부. 하나라도 빠지면 거절한다
 */
public record ScheduleReorderRequest(
        Long parentId,

        @NotEmpty(message = "ids는 비울 수 없습니다") List<Long> ids) {
}
