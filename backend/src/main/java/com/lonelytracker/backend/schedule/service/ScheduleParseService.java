package com.lonelytracker.backend.schedule.service;

import com.lonelytracker.backend.ai.AiParseCommand;
import com.lonelytracker.backend.ai.AiTarget;
import com.lonelytracker.backend.ai.AiTargetResolver;
import com.lonelytracker.backend.ai.AiUsageRecorder;
import com.lonelytracker.backend.ai.ParseResult;
import com.lonelytracker.backend.ai.ParsedSchedule;
import com.lonelytracker.backend.ai.ScheduleParser;
import com.lonelytracker.backend.common.exception.AiParseException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 자연어를 일정 초안으로 바꾼다. 초안은 별개로 저장하지 않는다.
 * 클래스에 {@code @Transactional} 을 두지 않아 수 초 걸리는 LLM 호출이 DB 커넥션을 잡지 않는다.
 */
@Service
@RequiredArgsConstructor
public class ScheduleParseService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleParseService.class);

    private final ScheduleParser scheduleParser;
    private final ScheduleService scheduleService;
    private final AiTargetResolver targetResolver;
    private final AiUsageRecorder usageRecorder;

    /**
     * 문장 하나를 초안 목록으로 바꾼다.
     *
     * @return 읽어낸 초안들. 쓸 수 없는 것은 버리고 남은 것만 온다
     */
    public List<ParsedSchedule> parse(String text) {
        // 짧은 트랜잭션. 여기서 닫힌다
        AiTarget target = targetResolver.resolve();

        List<String> knownTags = scheduleService.findTagNames();

        // 트랜잭션 밖에서 호출
        ParseResult result = scheduleParser.parse(
                new AiParseCommand(text, LocalDateTime.now(), knownTags,
                        target.baseUrl(), target.model(), target.apiKey()));

        // 결과를 받았으면 토큰은 쓴 것이다. 쓸 만한 초안이 없어도 적는다
        // 기록이 실패해도 초안은 돌려준다
        try {
            usageRecorder.record(target.baseUrl(), target.model(), result.usage());
        } catch (RuntimeException e) {
            log.warn("AI 사용량을 기록하지 못함", e);
        }

        // LLM 응답을 사용자 입력과 같은 등급으로 검증한다.
        // 하나가 어긋났다고 나머지까지 버리지 않는다
        List<ParsedSchedule> usable = result.schedules().stream()
                .filter(ScheduleParseService::isUsable)
                .map(ScheduleParseService::trim)
                .toList();

        if (usable.isEmpty()) {
            throw new AiParseException("일정으로 읽을 수 없는 문장입니다. 직접 입력해 주세요");
        }
        return usable;
    }

    /**
     * 쓸 수 있는 초안인지. 빈 칸은 잘못이 아니라 되물음의 대상이다.
     * 태그는 자유 입력이라 후보에 없는 이름도 그대로 둔다.
     */
    private static boolean isUsable(ParsedSchedule parsed) {
        if (parsed.title() == null || parsed.title().isBlank()) {
            return false;
        }

        // 끝이 시작보다 이르면 그 초안은 고칠 방법이 없다
        return parsed.startAt() == null || parsed.endAt() == null
                || !parsed.endAt().isBefore(parsed.startAt());
    }

    private static ParsedSchedule trim(ParsedSchedule parsed) {
        return new ParsedSchedule(
                parsed.title().strip(),
                parsed.startAt(),
                parsed.endAt(),
                parsed.allDay(),
                parsed.tags(),
                parsed.place(),
                parsed.recurrence(),
                parsed.questions());
    }
}
