package com.lonelytracker.backend.ai;

import com.lonelytracker.backend.common.AppProperties;
import com.lonelytracker.backend.common.exception.AiParseException;
import com.lonelytracker.backend.common.exception.AiUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

/**
 * AI 제공자에게 한 번 묻고 결과 JSON 을 돌려준다.
 * 이 프로젝트에서 HTTP 를 직접 다루는 유일한 클래스다.
 *
 * <p>무엇을 묻는지는 부르는 쪽이 정하고, 여기서는 규약 고르기·재시도·봉투 해체만 한다.
 * 일정 파서와 습관 파서가 이 기계를 나눠 쓴다 — 재시도 간격이나 오류 문구가 둘로 갈리면
 * 한쪽만 고쳐 두고 다른 쪽을 잊는다.
 */
@Component
public class AiCaller {

    /** 재시도 간격의 시작값 */
    private static final long BACKOFF_MILLIS = 1_000L;

    /** 오류 메시지에 실을 응답 원문의 최대 길이 */
    private static final int HINT_LENGTH = 300;

    private final AppProperties.AiSetting setting;
    private final ObjectMapper mapper;
    private final RestClient client;

    /** @param client 배선은 {@link AiClientConfig} 가 맡는다 */
    public AiCaller(AppProperties properties, ObjectMapper mapper, RestClient client) {
        this.setting = properties.ai();
        this.mapper = mapper;
        this.client = client;
    }

    /**
     * 주소를 보고 규약을 고른다.
     * Claude 는 호환 계층이 strict 를 무시해 네이티브로만 부른다
     */
    static AiProtocol protocolFor(String baseUrl) {
        String host = (baseUrl == null) ? "" : baseUrl.toLowerCase(Locale.ROOT);
        return host.contains("api.anthropic.com")
                ? new ClaudeMessagesProtocol()
                : new ChatCompletionsProtocol();
    }

    /**
     * 한 번 묻고 결과를 받는다.
     *
     * @return 결과 JSON 과 그 호출이 쓴 토큰
     */
    Answer ask(String baseUrl, String model, String apiKey, Prompt prompt) {
        AiProtocol protocol = protocolFor(baseUrl);
        Map<String, Object> body = protocol.body(model, prompt.system(), prompt.user(),
                prompt.schema());

        String envelope = callWithRetry(protocol, baseUrl, body, apiKey);
        return new Answer(extractOutput(protocol, envelope), usageOf(protocol, envelope));
    }

    /**
     * 보낼 말.
     *
     * @param system 규칙과 예시
     * @param user   사용자가 친 문장
     * @param schema 받을 모양. 어떤 뿌리를 쓸지는 규약이 고른다
     */
    record Prompt(String system, String user, AiSchema schema) {
    }

    /**
     * 받은 것.
     *
     * @param output 결과 JSON 트리. 해석은 부르는 쪽이 한다
     */
    record Answer(JsonNode output, AiUsage usage) {
    }

    /** 오류 메시지에 실을 만큼으로 줄인다 */
    static String hint(String json) {
        String flat = json.replaceAll("\\s+", " ");
        return (flat.length() <= HINT_LENGTH) ? flat : flat.substring(0, HINT_LENGTH) + "...";
    }

    // --- HTTP ------------------------------------------------------------

    /** 요청을 보내고 응답 본문을 돌려준다. 일시적 실패(5xx·429)면 백오프 후 재시도한다 */
    private String callWithRetry(AiProtocol protocol, String baseUrl, Map<String, Object> body,
            String apiKey) {
        RestClientException lastFailure = null;

        for (int attempt = 0; attempt <= setting.maxRetries(); attempt++) {
            try {
                RestClient.RequestBodySpec request = client.post()
                        .uri(stripTrailingSlash(baseUrl) + protocol.path())
                        .contentType(MediaType.APPLICATION_JSON);
                protocol.authHeaders(apiKey).forEach(request::header);

                return request
                        .body(body)
                        .retrieve()
                        // 429 는 일시적 실패라 아래 catch 로 흘려보낸다
                        .onStatus(status -> status.is4xxClientError()
                                && status.value() != HttpStatus.TOO_MANY_REQUESTS.value(),
                                (req, res) -> {
                                    throw new AiParseException("AI 요청이 거부되었습니다 ("
                                            + res.getStatusCode().value() + ") " + reasonOf(res));
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

    /** 사용량을 못 읽어도 결과는 살린다 */
    private AiUsage usageOf(AiProtocol protocol, String envelope) {
        try {
            return protocol.usageOf(mapper.readTree(envelope));
        } catch (RuntimeException e) {
            return AiUsage.NONE;
        }
    }

    /** 주소 끝의 / 와 규약 경로의 / 가 겹치지 않게 한다 */
    private static String stripTrailingSlash(String baseUrl) {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /**
     * 거절한 쪽이 남긴 이유.
     * 제공자마다 본문 모양이 달라 message 를 먼저 보고 없으면 원문을 줄여 싣는다
     */
    private String reasonOf(ClientHttpResponse response) {
        String body;
        try {
            body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "(이유를 읽지 못했습니다)";
        }
        if (body.isBlank()) {
            return "(본문 없음)";
        }

        try {
            String message = mapper.readTree(body).path("error").path("message").asString("");
            if (!message.isBlank()) {
                return hint(message);
            }
        } catch (RuntimeException ignored) {
            // JSON 이 아니면 원문을 그대로 줄여 싣는다
        }
        return hint(body);
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

    // --- 봉투 해체 --------------------------------------------------------

    /**
     * 응답 봉투에서 결과 JSON 을 꺼낸다.
     * 봉투 모양은 규약이 알고, 여기서는 그 안의 문자열을 트리로 만든다
     *
     * @throws AiParseException 결과를 못 찾았을 때
     */
    JsonNode extractOutput(AiProtocol protocol, String envelope) {
        JsonNode root;
        try {
            root = mapper.readTree(envelope);
        } catch (RuntimeException e) {
            throw new AiParseException("AI 응답을 읽지 못했습니다", e);
        }

        return readOutputJson(protocol.resultTextOf(root));
    }

    /** 봉투 안의 결과 문자열을 JSON 트리로 만든다 */
    private JsonNode readOutputJson(String text) {
        try {
            return mapper.readTree(text);
        } catch (RuntimeException e) {
            throw new AiParseException("AI 가 만든 JSON 을 읽지 못했습니다", e);
        }
    }
}
