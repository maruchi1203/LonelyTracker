package com.lonelytracker.backend.ai;

import java.util.List;

/**
 * 파싱 결과와 그 호출이 쓴 토큰
 *
 * @param schedules 문장에서 읽어낸 초안들. 하나뿐이면 길이 1의 목록이다
 */
public record ParseResult(List<ParsedSchedule> schedules, AiUsage usage) {
}
