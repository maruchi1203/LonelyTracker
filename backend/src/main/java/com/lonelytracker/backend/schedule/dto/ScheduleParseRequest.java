package com.lonelytracker.backend.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param text 자연어 한 줄. 예: "매주 월수금 아침 7시 헬스장에서 운동"
 *             토큰 비용이 입력 길이를 따라가므로 상한을 둔다
 */
public record ScheduleParseRequest(
        @NotBlank(message = "text는 필수입니다")
        @Size(max = 500, message = "text는 500자를 넘을 수 없습니다")
        String text
) {
}
