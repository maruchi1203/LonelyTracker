package com.lonelytracker.backend.ai;

/**
 * 자연어를 일정 초안으로 바꿈
 * 실제로는 필요없지만, 테스트용으로
 */
public interface ScheduleParser {
    ParsedSchedule parse(CommandForAI command);
}
