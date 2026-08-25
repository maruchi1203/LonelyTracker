package com.lonelytracker.backend.schedule.dto;

import com.lonelytracker.backend.common.FieldLengths;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 이 회차만 수정.
 * <p>
 * <b>모든 필드가 선택이다.</b> null 을 주면 그 필드는 일정(schedule) 값으로 되돌아간다.
 * 덮어쓰기 테이블이므로 "값이 없다" 가 곧 "일정을 따른다" 는 뜻이고,
 * 그래야 개별 수정을 취소할 수단이 생긴다.
 */
public record OccurrenceUpdateRequest(
        @Size(max = FieldLengths.TITLE, message = "title은 200자를 넘을 수 없습니다")
        String title,

        @Size(max = FieldLengths.DESCRIPTION, message = "description은 20000자를 넘을 수 없습니다")
        String description,

        LocalDateTime startAt,

        LocalDateTime endAt,

        @Size(max = FieldLengths.CATEGORY_NAME, message = "category는 50자를 넘을 수 없습니다")
        String category
) {
}
