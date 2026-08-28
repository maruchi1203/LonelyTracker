package com.lonelytracker.backend.ai;

/**
 * 자연어를 일정 초안으로 바꿈
 * OpenAI, Google, Claude API와 테스트용
 */
public interface ScheduleParser {
    ParsedSchedule parse(CommandForAI command);
}
