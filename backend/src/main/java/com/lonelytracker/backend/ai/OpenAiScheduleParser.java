package com.lonelytracker.backend.ai;

import com.lonelytracker.backend.common.AppProperties;
import com.lonelytracker.backend.common.exception.AiParseException;
import com.lonelytracker.backend.common.exception.AiUnavailableException;
import com.lonelytracker.backend.schedule.domain.ScheduleRecurrenceFreq;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * OpenAI Responses API 로 자연어를 일정 초안으로 바꾼다.
 * 이 프로젝트에서 HTTP 를 직접 다루는 유일한 클래스다.
 */
@Component
public class OpenAiScheduleParser implements ScheduleParser {

    /** 재시도 간격의 시작값 */
    private static final long BACKOFF_MILLIS = 1_000L;

    private final AppProperties.AiSetting setting; // AI용 기본 세팅
    private final ObjectMapper mapper; // JSON 직렬/역직렬용 객체
    private final RestClient client;

    /** @param client 배선은 {@link OpenAiClientConfig} 가 맡는다 */
    public OpenAiScheduleParser(AppProperties properties, ObjectMapper mapper, RestClient client) {
        this.setting = properties.ai();
        this.mapper = mapper;
        this.client = client;
    }

    @Override
    public ParsedSchedule parse(CommandForAI command) {
        String responseBody = callWithRetry(requestBody(command), command.apiKey());
        return toParsed(extractOutput(responseBody));
    }

    // --- HTTP ------------------------------------------------------------

    /** 요청을 보내고 응답 본문을 돌려준다. 일시적 실패(5xx·429)면 백오프 후 재시도한다. */
    private String callWithRetry(Map<String, Object> body, String apiKey) {
        RestClientException lastFailure = null;

        for (int attempt = 0; attempt <= setting.maxRetries(); attempt++) {
            try {
                return client.post()
                        .uri("/responses")
                        .header("Authorization", "Bearer " + apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(body)
                        .retrieve()
                        // 429 는 일시적 실패라 아래 catch 로 흘려보낸다
                        .onStatus(status -> status.is4xxClientError()
                                && status.value() != HttpStatus.TOO_MANY_REQUESTS.value(),
                                (req, res) -> {
                                    throw new AiParseException(
                                            "AI 요청이 거부되었습니다 (" + res.getStatusCode().value() + ")");
                                })
                        .body(String.class);
            } catch (AiParseException e) {
                throw e; // 4xx 는 재시도하지 않는다
            } catch (RestClientException e) {
                lastFailure = e;
                sleepBackoff(attempt);
            }
        }

        throw new AiUnavailableException("AI 응답을 받지 못했습니다", lastFailure);
    }

    /** 재시도 간격을 1초 → 2초 → 4초로 늘려 가며 기다린다 */
    private void sleepBackoff(int attempt) {
        try {
            Thread.sleep(BACKOFF_MILLIS << attempt);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new AiUnavailableException("AI 호출이 중단되었습니다", ie);
        }
    }

    /**
     * 응답 봉투에서 결과 JSON을 꺼낸다.
     *
     * @param envelope Responses API 응답 원문
     * @throws com.lonelytracker.backend.common.exception.AiParseException 결과를 못 찾았을 때
     */
    JsonNode extractOutput(String envelope) {
        JsonNode root;

        try {
            root = mapper.readTree(envelope);
        } catch (RuntimeException e) {
            throw new AiParseException("AI 응답을 읽지 못했습니다", e);
        }

        for (JsonNode item : root.path("output")) {
            if (!"message".equals(item.path("type").asString(""))) {
                continue;
            }
            for (JsonNode part : item.path("content")) {
                if ("output_text".equals(part.path("type").asString(""))) {
                    String text = part.path("text").asString("");
                    if (!text.isBlank()) {
                        return readOutputJson(text);
                    }
                }
            }
        }

        List<String> types = new ArrayList<>();
        root.path("output").forEach(item -> types.add(item.path("type").asString("?")));
        throw new AiParseException(
                "AI 응답에서 결과를 찾지 못했습니다. output 항목: " + types);
    }

    /** 봉투 안의 결과 문자열을 JSON 트리로 만든다 */
    private JsonNode readOutputJson(String text) {
        try {
            return mapper.readTree(text);
        } catch (RuntimeException e) {
            throw new AiParseException("AI 가 만든 JSON 을 읽지 못했습니다", e);
        }
    }

    // --- 요청 조립 --------------------------------------------------------
    private Map<String, Object> requestBody(CommandForAI command) {
        return Map.of(
                "model", setting.model(),
                "input", List.of(
                        Map.of("role", "system", "content",
                                systemPrompt(command.now(), command.categories())),
                        Map.of("role", "user", "content", command.text())),
                "text", Map.of("format", Map.of(
                        "type", "json_schema",
                        "name", "parsed_schedule",
                        "strict", true,
                        "schema", ParsedScheduleSchema.getSchedule())));
    }

    /** 규칙과 예시를 담은 system 메시지를 만든다. 칸별 규칙은 {@link ParsedScheduleSchema} 에 있다. */
    private String systemPrompt(LocalDateTime now, List<String> categories) {
        String categoryList = categories.isEmpty() ? "(없음)" : String.join(", ", categories);

        return """
                너는 한국어 일정 문장을 구조화된 JSON으로 바꾸는 도구다.
                칸별 규칙은 스키마의 description을 따른다.

                - 모르는 값은 지어내지 말고 null로 두고, 그 칸의 ID를 questions에 넣는다.
                - 행동이 막연하면(예: "열심히 하기") TOO_VAGUE를 넣거나 질문을 요청한다.

                예시 — 현재 시각이 2026-08-27T13:00:00 목요일, 분류 목록이 [육체] 일 때:
                "내일 3시 헬스장에서 운동"
                  title=운동 startAt=2026-08-28T15:00:00 category=육체 place=헬스장
                "매주 월수금 아침 7시 헬스장에서 운동"
                  startAt=2026-08-31T07:00:00
                  recurrence={"freq":"WEEKLY","byWeekday":["MONDAY","WEDNESDAY","FRIDAY"],"endsOn":null}
                "회의"
                  title=회의 startAt=null questions=["DATE","START_TIME","PLACE"]

                현재 시각: %s (%s) — 상대 날짜는 이 시각 기준으로 푼다.
                분류 목록: %s
                """
                .formatted(now, koreanDayOfWeek(now.getDayOfWeek()), categoryList);
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

    /** 오류 메시지에 실을 응답 원문의 최대 길이 */
    private static final int HINT_LENGTH = 300;

    private ParsedSchedule toParsed(JsonNode node) {
        // 제목과 시작일을 못 채우면 최소한의 일정을 형성할 수 없음
        if (textOrNull(node, "title") == null || dateTimeOrNull(node, "startAt") == null) {
            throw new AiParseException("AI가 제목을 채우지 못했습니다. 응답: " + hint(node.toString()));
        }

        return new ParsedSchedule(
                textOrNull(node, "title"),
                dateTimeOrNull(node, "startAt"),
                dateTimeOrNull(node, "endAt"),
                node.path("allDay").asBoolean(false),
                textOrNull(node, "category"),
                textOrNull(node, "place"),
                recurringOf(node.path("recurrence")),
                questionsOf(node.path("questions")));
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

    private String hint(String json) {
        String flat = json.replaceAll("\s+", " ");
        return (flat.length() <= HINT_LENGTH) ? flat : flat.substring(0, HINT_LENGTH) + "...";
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
