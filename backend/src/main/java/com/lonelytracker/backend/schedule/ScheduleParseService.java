package com.lonelytracker.backend.schedule;

import com.lonelytracker.backend.ai.CommandForAI;
import com.lonelytracker.backend.ai.ParsedSchedule;
import com.lonelytracker.backend.ai.ScheduleParser;
import com.lonelytracker.backend.common.exception.AiParseException;
import com.lonelytracker.backend.common.exception.AiUnavailableException;
import com.lonelytracker.backend.user.CurrentUserProvider;
import com.lonelytracker.backend.user.User;
import com.lonelytracker.backend.user.UserCategoryService;
import com.lonelytracker.backend.user.dto.CategoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 자연어 → 일정 초안. <b>저장하지 않는다.</b>
 * <p>
 * 이 클래스에 {@code @Transactional} 이 <b>없는 것이 설계다.</b>
 * LLM 응답은 수 초가 걸리는데, 트랜잭션 안에서 부르면 그동안 DB 커넥션을 붙잡는다.
 * 커넥션 풀이 보통 10개라 동시에 열 명이 요청하면 앱 전체가 멈춘다.
 * 
 * <pre>
 * [트랜잭션] 카테고리 읽기 [닫힘]  →  LLM 호출 수 초  →  검증  →  응답
 *              ~5ms                    커넥션 안 잡음
 * </pre>
 * 
 * 카테고리를 읽을 때는 트랜잭션이 열리지만 <b>LLM 호출 전에 닫힌다</b>.
 * {@code propagation = NOT_SUPPORTED} 로 뚫을 수도 있으나 그 한 줄의 의미를
 * 놓치기 쉬워, 클래스를 나눠 구조로 보장한다.
 */
@Service
@RequiredArgsConstructor
public class ScheduleParseService {

    private final ScheduleParser scheduleParser;
    private final UserCategoryService userCategoryService;
    private final CurrentUserProvider currentUserProvider;

    public ParsedSchedule parse(String text) {
        // ① 짧은 트랜잭션. 여기서 닫힌다
        User user = currentUserProvider.get();
        if (!user.hasOpenAiApiKey()) {
            // 서버 설정이 아니라 이 사용자가 키를 등록하지 않은 것이다.
            // 사용자 잘못이 아니므로 4xx 가 아니고, 나머지 기능은 그대로 쓴다.
            throw new AiUnavailableException(
                    "OpenAI API 키를 먼저 등록해 주세요. 등록 전에는 직접 입력해 주세요");
        }
        String apiKey = user.getOpenAiApiKey();

        List<String> categories = userCategoryService.findAll().stream()
                .filter(c -> !c.archived())
                .map(CategoryResponse::name)
                .toList();

        // ② 트랜잭션 밖에서 호출
        ParsedSchedule parsed = scheduleParser.parse(
                new CommandForAI(text, LocalDateTime.now(), categories, apiKey));

        // ③ LLM 응답을 사용자 입력과 같은 등급으로 검증한다
        return validate(parsed, categories);
    }

    /**
     * 형식이 맞아도 내용이 틀릴 수 있다.
     * <p>
     * 여기서 거르지 않으면 화면이 이상한 초안을 그대로 보여주게 된다.
     * 다만 <b>비어 있는 것은 잘못이 아니다</b> — 모르면 null 로 두라고 시켰고,
     * 그 자리는 질문으로 채운다.
     */
    private ParsedSchedule validate(ParsedSchedule parsed, List<String> categories) {
        if (parsed.title() == null || parsed.title().isBlank()) {
            throw new AiParseException("일정으로 읽을 수 없는 문장입니다. 직접 입력해 주세요");
        }
        if (parsed.startAt() != null && parsed.endAt() != null
                && parsed.endAt().isBefore(parsed.startAt())) {
            throw new AiParseException("AI 가 종료 시각을 시작보다 이르게 잡았습니다");
        }

        // 목록에 없는 분류를 지어냈으면 버린다. 목록 안에서만 고르라고 시켰다
        String category = (parsed.category() != null && !categories.contains(parsed.category()))
                ? null
                : parsed.category();

        return new ParsedSchedule(
                parsed.title().strip(),
                parsed.startAt(),
                parsed.endAt(),
                parsed.allDay(),
                category,
                parsed.place(),
                parsed.recurrence(),
                parsed.questions());
    }
}
