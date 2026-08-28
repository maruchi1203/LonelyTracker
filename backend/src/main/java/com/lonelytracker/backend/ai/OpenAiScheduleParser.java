package com.lonelytracker.backend.ai;

import com.lonelytracker.backend.common.AppProperties;
import com.lonelytracker.backend.common.exception.AiParseException;
import com.lonelytracker.backend.common.exception.AiUnavailableException;
import com.lonelytracker.backend.schedule.RecurrenceFreq;

import org.springframework.http.HttpStatusCode;
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
 * <p>
 * <b>HTTP 를 아는 유일한 클래스다.</b> Spring AI 같은 추상화를 쓰지 않는 이유는,
 * 타임아웃·재시도·검증을 어떻게 설계했는지가 이 프로젝트에서 보여줄 판단이기 때문이다.
 */
@Component
public class OpenAiScheduleParser implements ScheduleParser {

    /** 재시도 간격의 시작값. 즉시 재시도하면 과부하 상황을 악화시킨다 */
    private static final long BACKOFF_MILLIS = 1_000L;

    private final AppProperties.AiSetting setting; // AI용 기본 세팅
    private final ObjectMapper mapper; // JSON 직렬/역직렬용 객체
    private final RestClient client;

    /**
     * @param client 배선은 {@link OpenAiClientConfig} 가 한다. 밖에서 받는 덕에
     *               테스트에서 가짜 서버를 끼울 수 있다 — 재시도 규칙은 실제
     *               상태 코드를 받아봐야 검증된다.
     */
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

    /**
     * 재시도는 일시적 실패에만 한다.
     * 4xx 는 우리 요청이 잘못된 것이라 몇 번을 보내도 같은 결과다.
     */
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
                        .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
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

    /** 호출 연기 시간 1초 → 2초 → 4초 → ... **/
    private void sleepBackoff(int attempt) {
        try {
            Thread.sleep(BACKOFF_MILLIS << attempt);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new AiUnavailableException("AI 호출이 중단되었습니다", ie);
        }
    }

    /**
     * 1. API 응답 전체 텍스트 → JSON 변환 필요한 부분만 추출 →
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
                        // 봉투 안에는 결과가 "문자열" 로 들어 있다.
                        // 여기서 한 번만 풀어 다음 층에 트리로 넘긴다.
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

    /** 봉투 안의 결과 문자열을 트리로 만든다. 실패하면 봉투 문제와 구분되는 메시지를 낸다. */
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

    /**
     * 규칙은 system 에, 사용자 입력은 user 에 둔다.
     * 섞으면 <b>사용자 입력이 규칙을 덮어쓸 수 있다</b>.
     */
    private String systemPrompt(LocalDateTime now, List<String> categories) {
        String categoryList = categories.isEmpty() ? "(없음)" : String.join(", ", categories);

        return """
                너는 한국어 일정 문장을 구조화된 JSON 으로 바꾸는 도구다.

                현재 시각: %s (%s)
                사용자의 분류 목록: %s

                규칙:
                - 날짜와 시각은 현재 시각을 기준으로 해석한다.
                - 시각 형식은 반드시 2026-08-25T14:30:00 처럼 쓴다. 타임존을 붙이지 않는다.
                - 분류는 위 목록에 있는 이름만 쓴다. 맞는 것이 없으면 null 이다.
                - 모르는 값은 지어내지 말고 null 로 둔다.
                - 채우지 못한 칸이 있으면 questions 에 해당 ID 를 넣는다.
                - 장소가 문장에 있으면 place 에 넣고, 없으면 null 로 두고 PLACE 를 묻는다.
                - 반복이면 recurrence 를 채우고, 한 번뿐이면 null 로 둔다.
                - 행동이 막연하면(예: "열심히 하기") TOO_VAGUE 를 넣는다.

                예시:
                입력: "내일 3시 헬스장에서 운동"
                출력: title=운동, startAt=(내일 15:00), place=헬스장, recurrence=null

                입력: "매주 월수금 아침 7시 헬스장"
                출력: recurrence.freq=WEEKLY, byWeekday=[MONDAY,WEDNESDAY,FRIDAY], startAt=(다음 월요일 07:00)

                입력: "회의"
                출력: title=회의, startAt=null, questions=[DATE, START_TIME, PLACE]
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

    /** 진단용. 원문을 통째로 노출하지 않되 무엇이 왔는지는 보이게 한다. */
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

    // 반복 일정 파싱
    private ParsedRecurringSchedule recurringOf(JsonNode node) {
        // Node가 없을 경우
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
                RecurrenceFreq.valueOf(node.path("freq").asString()),
                weekdays,
                (endsOn == null) ? null : LocalDate.parse(endsOn));

        return schedule;
    }

    /**
     * 질문 ID를 반환받아 Parsing
     */
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
