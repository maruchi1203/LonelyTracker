package com.lonelytracker.backend.schedule.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 형제 무리를 통째로 다시 세운다.
 * 받은 차례대로 0부터 번호를 다시 매긴다.
 *
 * @param parentId null이면 최상위 무리다
 * @param ids      그 무리의 최종 구성원 전부. 밖에 있던 일정은 데려오고, 원래 있던 것이 빠지면 거절한다
 */
public record ScheduleReorderRequest(
        Long parentId,

        @NotEmpty(message = "ids는 비울 수 없습니다") List<Long> ids) {
}
