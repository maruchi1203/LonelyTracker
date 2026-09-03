package com.lonelytracker.backend.ai;

/**
 * 자연어를 일정 초안으로 바꾼다.
 * 제공자를 바꿔 끼울 수 있도록 인터페이스로 둔다.
 */
public interface ScheduleParser {
    ParsedSchedule parse(AiParseCommand command);
}
