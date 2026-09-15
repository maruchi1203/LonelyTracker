package com.lonelytracker.backend.ai;

/**
 * 자연어를 일정 초안으로 바꾼다.
 * 제공자를 바꿔 끼울 수 있도록 인터페이스로 둔다.
 */
public interface ScheduleParser {

    /** 문장 하나에 일정이 여럿 들어 있을 수 있다. 그 호출이 쓴 토큰도 함께 온다 */
    ParseResult parse(AiParseCommand command);
}
