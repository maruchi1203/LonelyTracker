package com.lonelytracker.backend.ai;

import com.lonelytracker.backend.common.exception.AiParseException;
import com.lonelytracker.backend.schedule.domain.ScheduleRecurrenceFreq;

import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 자연어를 일정 초안으로 바꾼다.
 *
 * <p>물어보는 일은 {@link AiCaller} 가, 규약이 다른 부분은 {@link AiProtocol} 이 맡는다.
 * 여기 남은 것은 무엇을 물을지(프롬프트)와 받은 JSON 을 초안으로 바꾸는 일뼐이다.
 */
@Component
public class AiScheduleParser implements ScheduleParser {

    private final AiCaller caller;

    public AiScheduleParser(AiCaller caller) {
        this.caller = caller;
    }

    @Override
    public ParseResult parse(AiParseCommand command) {
        AiCaller.Answer answer = caller.ask(
                command.baseUrl(), command.model(), command.apiKey(),
                new AiCaller.Prompt(
                        systemPrompt(command.now(), command.knownTags()),
                        command.text(),
                        new ParsedScheduleSchema()));

        return new ParseResult(toParsedList(answer.output()), answer.usage());
    }

    // --- 요청 조립 --------------------------------------------------------

    /** 규칙과 예시를 담은 system 메시지를 만든다. 칸별 규칙은 {@link ParsedScheduleSchema} 에 있다. */
    private String systemPrompt(LocalDateTime now, List<String> knownTags) {
        String tagList = knownTags.isEmpty() ? "(없음)" : String.join(", ", knownTags);

        return """
                너는 한국어 일정 문장을 구조화된 JSON으로 바꾸는 도구다.
                칸별 규칙은 스키마의 description을 따른다.

                - 모르는 값은 지어내지 말고 null로 두고, 그 칸의 ID를 questions에 넣는다.
                - 행동이 막연하면(예: "열심히 하기") TOO_VAGUE를 넣거나 질문을 요청한다.
                - 문장에 서로 다른 일정이 여럿이면 schedules 배열에 하나씩 나눠 담는다.
                  한 일정을 쪼개지는 말고, 서로 다른 일을 한 칸에 합치지도 않는다.
                - tags 는 비워 두지 않는다. 후보에 맞는 것이 없으면 그 일이 어느 갈래인지
                  한 단어로 지어 붙인다. 예: 육체, 정신, 일, 관계, 집안일, 돈.

                예시 — 현재 시각이 2026-08-27T13:00:00 목요일, 태그 후보가 [육체] 일 때:
                "내일 3시 헬스장에서 운동"
                  title=운동 startAt=2026-08-28T15:00:00 tags=["육체"] place=헬스장
                "매주 월수금 아침 7시 헬스장에서 운동"
                  startAt=2026-08-31T07:00:00
                  recurrence={"freq":"WEEKLY","byWeekday":["MONDAY","WEDNESDAY","FRIDAY"],"endsOn":null}
                "다음주 월~수까지 아침 7시 운동"
                  startAt=2026-08-31T07:00:00
                  recurrence={"freq":"DAILY","byWeekday":[],"endsOn":"2026-09-02"}
                "회의"
                  title=회의 startAt=null questions=["DATE","START_TIME","PLACE"]
                "내일 3시 치과, 5시에 장보기"
                  schedules 에 두 칸 — title=치과 startAt=2026-08-28T15:00:00 tags=["건강"] 과
                  title=장보기 startAt=2026-08-28T17:00:00 tags=["집안일"]

                현재 시각: %s (%s) — 상대 날짜는 이 시각 기준으로 푼다.
                태그 후보: %s — 맞는 것이 있으면 쓰고, 없으면 새로 지어도 된다.
                """
                .formatted(now, koreanDayOfWeek(now.getDayOfWeek()), tagList);
    }

    private String koreanDayOfWeek(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "월요일";
            case TUESDAY -> "화요일";
            case WEDNESDAY -> "수요일";
            case THURSDAY -> "목요일";
            case FRIDAY -> "금요일";
            case SATURDAY -> "토요일";
            case SUNDAY -> "일요일";
        };
    }

    // --- 응답 해석 --------------------------------------------------------

    /**
     * 봉투 안의 배열을 초안 목록으로 바꾼다.
     * 하나도 못 읽으면 문장 자체를 일정으로 볼 수 없었던 것이다.
     */
    private List<ParsedSchedule> toParsedList(JsonNode root) {
        JsonNode schedules = root.path("schedules");
        if (!schedules.isArray() || schedules.isEmpty()) {
            throw new AiParseException(
                    "AI 가 일정을 하나도 읽지 못했습니다. 응답: " + AiCaller.hint(root.toString()));
        }

        List<ParsedSchedule> parsed = new ArrayList<>();
        for (JsonNode node : schedules) {
            if (parsed.size() >= ParsedScheduleSchema.MAX_SCHEDULES) {
                break;
            }
            parsed.add(toParsed(node));
        }
        return parsed;
    }

    private ParsedSchedule toParsed(JsonNode node) {
        // 제목을 못 채우면 최소한의 일정을 형성할 수 없음
        if (textOrNull(node, "title") == null) {
            throw new AiParseException("AI가 제목을 채우지 못했습니다. 응답: " + AiCaller.hint(node.toString()));
        }

        return new ParsedSchedule(
                textOrNull(node, "title"),
                dateTimeOrNull(node, "startAt"),
                dateTimeOrNull(node, "endAt"),
                node.path("allDay").asBoolean(false),
                tagsOf(node.path("tags")),
                textOrNull(node, "place"),
                recurringOf(node.path("recurrence")),
                questionsOf(node.path("questions")));
    }

    /** 초안의 태그. 빈 것과 공백은 버린다 */
    private List<String> tagsOf(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<String> tags = new ArrayList<>();
        for (JsonNode t : node) {
            String name = t.asString();
            if (name != null && !name.isBlank()) {
                tags.add(name.strip());
            }
        }
        return tags;
    }

    /** 초안의 반복 규칙. 없으면 null */
    private ParsedRecurringSchedule recurringOf(JsonNode node) {
        // 반복이 아니면 통째로 비어 있다
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }

        // 반복 요일
        Set<DayOfWeek> weekdays = EnumSet.noneOf(DayOfWeek.class);
        node.path("byWeekday").forEach(n -> weekdays.add(DayOfWeek.valueOf(n.asString())));

        // 종료일
        String endsOn = textOrNull(node, "endsOn");

        // 파싱된 스케쥴
        ParsedRecurringSchedule schedule = new ParsedRecurringSchedule(
                ScheduleRecurrenceFreq.valueOf(node.path("freq").asString()),
                weekdays,
                (endsOn == null) ? null : LocalDate.parse(endsOn));

        return schedule;
    }

    /** 되물음 ID 목록. 모르는 ID는 버린다 */
    private List<ParseQuestion> questionsOf(JsonNode node) {
        List<ParseQuestion> questions = new ArrayList<>();

        node.forEach(n -> {
            try {
                questions.add(ParseQuestion.valueOf(n.asString()));
            } catch (IllegalArgumentException ignored) {
                // 알 수 없는 질문 ID
            }
        });

        return questions;
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asString();
        return text.isBlank() ? null : text;
    }

    private LocalDateTime dateTimeOrNull(JsonNode node, String field) {
        String text = textOrNull(node, field);
        if (text == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(text);
        } catch (DateTimeParseException e) {
            throw new AiParseException("AI 가 준 시각을 읽지 못했습니다: " + text, e);
        }
    }
}
