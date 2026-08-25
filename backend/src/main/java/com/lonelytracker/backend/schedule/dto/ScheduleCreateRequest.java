package com.lonelytracker.backend.schedule.dto;

import com.lonelytracker.backend.common.FieldLengths;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 일정 생성. 단일과 반복이 같은 엔드포인트를 쓴다.
 *
 * @param startAt    단일이면 그 시각, 반복이면 <b>첫 회차</b>의 시각
 * @param endAt      소요시간으로 환산해 저장한다. null 이면 종료 시각 없음
 * @param recurrence <b>주면 반복, 안 주면 1회성</b>이다. 별도 플래그가 없는 이유다
 */
public record ScheduleCreateRequest(
        @NotBlank(message = "title은 필수입니다")
        @Size(max = FieldLengths.TITLE, message = "title은 200자를 넘을 수 없습니다")
        String title,

        // 마크다운 원문. 문서처럼 길어질 수 있어 넉넉하게 잡는다
        @Size(max = FieldLengths.DESCRIPTION, message = "description은 20000자를 넘을 수 없습니다")
        String description,

        @NotNull(message = "startAt은 필수입니다")
        LocalDateTime startAt,

        LocalDateTime endAt,

        // 원시 boolean이면 JSON에서 생략됐을 때 Jackson이 실패한다. 선택 필드라 래퍼 타입을 쓴다
        Boolean allDay,

        // 사용자의 카테고리 목록에 없는 이름도 허용한다. 목록은 후보일 뿐이다
        @Size(max = FieldLengths.CATEGORY_NAME, message = "category는 50자를 넘을 수 없습니다")
        String category,

        @Valid
        RecurrenceRequest recurrence
) {
}
