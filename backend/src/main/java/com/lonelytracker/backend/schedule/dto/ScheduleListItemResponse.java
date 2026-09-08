package com.lonelytracker.backend.schedule.dto;

import com.lonelytracker.backend.schedule.entity.ScheduleEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * 리스트 탭의 한 줄. 회차가 아니라 일정 자체다.
 * 습관은 이 목록에 오지 않아 회차도 상태도 싣지 않는다.
 * <p>
 * 계층은 {@code parentId} 만 싣고 트리는 화면이 조립한다.
 * 목록 안의 모든 부모가 같은 목록 안에 있어 한 번 훑으면 묶인다.
 *
 * @param dueOn       기한. 언제까지 해내야 하나
 * @param startAt     언제 하기로 했나. 없으면 아직 안 정한 항목이다
 * @param completedAt 값이 있으면 완료다
 */
public record ScheduleListItemResponse(
        Long id,
        Long parentId,
        int displayOrder,
        String title,
        String description,
        LocalDate dueOn,
        LocalDateTime startAt,
        LocalDateTime completedAt,
        Set<String> tags,
        String place,
        String twoMinuteAction,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static ScheduleListItemResponse from(ScheduleEntity s) {
        return new ScheduleListItemResponse(
                s.getId(),
                s.getParentId(),
                s.getDisplayOrder(),
                s.getTitle(),
                s.getDescription(),
                s.getDueOn(),
                s.getStartAt(),
                s.getCompletedAt(),
                // 지연 로딩 컬렉션을 그대로 내보내면 세션이 닫힌 뒤 직렬화가 깨진다
                s.tagsCopy(),
                s.getPlace(),
                s.getTwoMinuteAction(),
                s.getCreatedAt(),
                s.getUpdatedAt());
    }
}
