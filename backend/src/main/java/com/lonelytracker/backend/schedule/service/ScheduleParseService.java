package com.lonelytracker.backend.schedule.service;

import com.lonelytracker.backend.ai.AiParseCommand;
import com.lonelytracker.backend.ai.ParsedSchedule;
import com.lonelytracker.backend.ai.ScheduleParser;
import com.lonelytracker.backend.common.exception.AiParseException;
import com.lonelytracker.backend.common.exception.AiUnavailableException;
import com.lonelytracker.backend.user.service.UserProvider;
import com.lonelytracker.backend.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
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

    private final ScheduleParser scheduleParser;
    private final ScheduleService scheduleService;
    private final UserProvider currentUserProvider;

    /**
     * 문장 하나를 초안 목록으로 바꾼다.
     *
     * @return 읽어낸 초안들. 쓸 수 없는 것은 버리고 남은 것만 온다
     */
    public List<ParsedSchedule> parse(String text) {
        // 짧은 트랜잭션. 여기서 닫힌다
        UserEntity user = currentUserProvider.get();
        if (!user.hasOpenAiApiKey()) {
            // 서버 설정이 아니라 이 사용자가 키를 등록하지 않은 것이다
            throw new AiUnavailableException(
                    "OpenAI API 키를 먼저 등록해 주세요. 등록 전에는 직접 입력해 주세요");
        }
        String apiKey = user.getOpenAiApiKey();

        List<String> knownTags = scheduleService.findTagNames();

        // 트랜잭션 밖에서 호출
        List<ParsedSchedule> parsed = scheduleParser.parse(
                new AiParseCommand(text, LocalDateTime.now(), knownTags, apiKey));

        // LLM 응답을 사용자 입력과 같은 등급으로 검증한다.
        // 하나가 어긋났다고 나머지까지 버리지 않는다
        List<ParsedSchedule> usable = parsed.stream()
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
