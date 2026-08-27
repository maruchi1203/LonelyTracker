package com.lonelytracker.backend.ai;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 자연어에서 뽑아낸 일정 초안. <b>저장하지 않는다.</b>
 * <p>
 * 필드 이름을 {@code ScheduleCreateRequest} 와 맞춘다. 화면이 받은 것을 변환 없이
 * 폼에 꽂고 그대로 되쏠 수 있게 하려는 것이다.
 *
 * @param place     어디서 할 것인가. 습관 형성 지침의 "시간/장소" 중 장소.
 *                  <b>아직 저장할 컬럼이 없어 초안에만 실린다</b>
 * @param questions 모델이 채우지 못한 칸을 사용자에게 되물을 항목.
 *                  문구는 서버가 갖고 모델은 ID 만 고른다
 */
public record ParsedSchedule(
        String title,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean allDay,
        String category,
        String place,
        ParsedRecurrence recurrence,
        List<ParseQuestion> questions
) {
}
