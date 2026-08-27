package com.lonelytracker.backend.ai;

import com.lonelytracker.backend.common.AppProperties;
import com.lonelytracker.backend.common.exception.AiParseException;
import com.lonelytracker.backend.common.exception.AiUnavailableException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
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

    private final AppProperties.Ai config;
    private final ObjectMapper mapper;
    private final RestClient client;

    public OpenAiScheduleParser(AppProperties properties, ObjectMapper mapper) {
        this.config = properties.ai();
        this.mapper = mapper;

        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory();
        factory.setReadTimeout(config.readTimeout());

        this.client = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(config.baseUrl())
                .build();
    }

    @Override
    public ParsedSchedule parse(ParseCommand command) {
        String json = extractOutputText(callWithRetry(requestBody(command), command.apiKey()));
        return toParsed(json);
    }

    // --- HTTP ------------------------------------------------------------

    /**
     * 재시도는 <b>일시적 실패에만</b> 한다.
     * 4xx 는 우리 요청이 잘못된 것이라 몇 번을 보내도 같은 결과다.
     */
    private String callWithRetry(Map<String, Object> body, String apiKey) {
        // 던질 예외를 미리 담아두면 초기화 여부를 컴파일러가 증명하지 못해
        // null 이나 억지 캐스트를 넣게 된다. 원인만 들고 있다가 나갈 때 새로 만든다.
        RestClientException lastFailure = null;

        for (int attempt = 0; attempt <= config.maxRetries(); attempt++) {
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

    /** 지수 백오프. 1초 → 2초 → 4초. 모두가 즉시 재시도하면 상황이 더 나빠진다. */
    private void sleepBackoff(int attempt) {
        try {
            Thread.sleep(BACKOFF_MILLIS << attempt);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new AiUnavailableException("AI 호출이 중단되었습니다", ie);
        }
    }

    /**
     * Responses API 는 봉투 안에 결과를 <b>문자열로</b> 담는다.
     * 그래서 두 번 파싱한다 — 봉투 한 번, 그 안의 문자열 한 번.
     * <p>
     * <b>인덱스로 찍으면 안 된다.</b> {@code output} 배열의 첫 항목은 대개
     * {@code type: "reasoning"} 이고 실제 답은 그 뒤의 {@code type: "message"} 에 있다.
     * 모델이나 옵션에 따라 앞에 붙는 항목 수가 달라지므로 <b>타입으로 찾는다</b>.
     */
    private String extractOutputText(String envelope) {
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
                        return text;
                    }
                }
            }
        }

        // 무엇이 왔는지 알 수 없으면 진단이 불가능하다. 온 항목의 타입만 알려준다.
        List<String> types = new ArrayList<>();
        root.path("output").forEach(item -> types.add(item.path("type").asString("?")));
        throw new AiParseException(
                "AI 응답에서 결과를 찾지 못했습니다. output 항목: " + types);
    }

    // --- 요청 조립 --------------------------------------------------------

    /**
     * <b>문자열이 아니라 Map 을 돌려준다.</b> 직렬화를 Jackson 컨버터에 맡기기 위해서다.
     * <p>
     * 직접 문자열로 만들어 {@code .body(String)} 으로 넘기면 인코딩이
     * {@code Content-Type} 의 charset 에 좌우된다. {@code application/json} 에는
     * charset 이 없어 한글이 깨질 수 있다. Map 을 넘기면 Jackson 컨버터가
     * <b>UTF-8 로 직렬화</b>하므로 그 위험이 사라진다.
     */
    private Map<String, Object> requestBody(ParseCommand command) {
        return Map.of(
                "model", config.model(),
                "input", List.of(
                        Map.of("role", "system", "content",
                                systemPrompt(command.now(), command.categories())),
                        Map.of("role", "user", "content", command.text())),
                "text", Map.of("format", Map.of(
                        "type", "json_schema",
                        "name", "parsed_schedule",
                        "strict", true,
                        "schema", ParsedScheduleSchema.get())));
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

    private ParsedSchedule toParsed(String json) {
        JsonNode node;
        try {
            node = mapper.readTree(json);
        } catch (RuntimeException e) {
            throw new AiParseException("AI 가 만든 JSON 을 읽지 못했습니다", e);
        }

        // 제목조차 없으면 쓸 수 있는 일정이 아니다. 무엇이 왔는지 남기지 않으면
        // "일정으로 읽을 수 없다" 만 보이고 어디가 어긋났는지 알 길이 없다.
        if (textOrNull(node, "title") == null) {
            throw new AiParseException(
                    "AI 가 제목을 채우지 못했습니다. 응답: " + hint(json));
        }

        return new ParsedSchedule(
                textOrNull(node, "title"),
                dateTimeOrNull(node, "startAt"),
                dateTimeOrNull(node, "endAt"),
                node.path("allDay").asBoolean(false),
                textOrNull(node, "category"),
                textOrNull(node, "place"),
                recurrenceOf(node.path("recurrence")),
                questionsOf(node.path("questions")));
    }

    private ParsedRecurrence recurrenceOf(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        Set<DayOfWeek> weekdays = EnumSet.noneOf(DayOfWeek.class);
        node.path("byWeekday").forEach(n -> weekdays.add(DayOfWeek.valueOf(n.asString())));

        String endsOn = textOrNull(node, "endsOn");
        return new ParsedRecurrence(
                com.lonelytracker.backend.schedule.RecurrenceFreq.valueOf(
                        node.path("freq").asString()),
                weekdays,
                (endsOn == null) ? null : LocalDate.parse(endsOn));
    }

    /**
     * 모르는 ID 는 조용히 버린다. 스키마의 enum 이 막아주지만,
     * 모델이 규칙을 어겼을 때 그 하나 때문에 전체가 실패하면 안 된다.
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
