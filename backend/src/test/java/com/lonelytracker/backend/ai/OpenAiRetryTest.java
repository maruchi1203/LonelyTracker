package com.lonelytracker.backend.ai;

import com.lonelytracker.backend.common.AppProperties;
import com.lonelytracker.backend.common.exception.AiParseException;
import com.lonelytracker.backend.common.exception.AiUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 재시도 규칙을 가짜 HTTP 서버에 붙여 실제 상태 코드로 검증한다.
 * Spring 컨텍스트를 띄우지 않으므로 Docker 없이 돈다.
 */
class OpenAiRetryTest {

    private static final String ENDPOINT = "http://ai.test/responses";

    /** 성공 응답의 최소 형태. 봉투 모양은 OpenAiResponseShapeTest 가 본다. */
    private static final String OK_BODY = """
            { "output": [ { "type": "message", "content": [
                { "type": "output_text",
                  "text": "{\\"title\\":\\"운동\\",\\"startAt\\":\\"2026-09-01T07:00:00\\"}" } ] } ] }""";

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    @DisplayName("429면 재시도해서 성공한다")
    void retriesOnTooManyRequests() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiScheduleParser parser = parserWith(builder, 1);

        server.expect(requestTo(ENDPOINT))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        server.expect(requestTo(ENDPOINT))
                .andRespond(withSuccess(OK_BODY, MediaType.APPLICATION_JSON));

        ParsedSchedule parsed = parser.parse(command());

        assertThat(parsed.title()).isEqualTo("운동");
        server.verify();
    }

    @Test
    @DisplayName("400이면 재시도하지 않는다")
    void doesNotRetryOnBadRequest() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiScheduleParser parser = parserWith(builder, 2);

        server.expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> parser.parse(command()))
                .isInstanceOf(AiParseException.class)
                .hasMessageContaining("400");

        server.verify();
    }

    @Test
    @DisplayName("401이면 재시도하지 않는다")
    void doesNotRetryOnUnauthorized() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiScheduleParser parser = parserWith(builder, 2);

        server.expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> parser.parse(command()))
                .isInstanceOf(AiParseException.class)
                .hasMessageContaining("401");
        server.verify();
    }

    @Test
    @DisplayName("5xx가 계속되면 상한만큼 시도한 뒤 503으로 끝난다")
    void givesUpAfterMaxRetries() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiScheduleParser parser = parserWith(builder, 1);

        // maxRetries=1 이면 최초 1회 + 재시도 1회 = 2회
        server.expect(requestTo(ENDPOINT))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        server.expect(requestTo(ENDPOINT))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> parser.parse(command()))
                .isInstanceOf(AiUnavailableException.class);
        server.verify();
    }

    @Test
    @DisplayName("maxRetries가 0이면 한 번만 시도한다")
    void noRetryWhenDisabled() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiScheduleParser parser = parserWith(builder, 0);

        server.expect(requestTo(ENDPOINT))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> parser.parse(command()))
                .isInstanceOf(AiUnavailableException.class);
        server.verify();
    }

    @Test
    @DisplayName("정상 응답이면 그대로 파싱된다")
    void parsesSuccessfulResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OpenAiScheduleParser parser = parserWith(builder, 2);

        server.expect(requestTo(ENDPOINT))
                .andRespond(withSuccess(OK_BODY, MediaType.APPLICATION_JSON));

        ParsedSchedule parsed = parser.parse(command());

        assertThat(parsed.title()).isEqualTo("운동");
        assertThat(parsed.startAt()).isEqualTo(LocalDateTime.parse("2026-09-01T07:00:00"));
        server.verify();
    }

    // --- 헬퍼 -------------------------------------------------------------

    /** 주어진 재시도 횟수로 파서를 만든다. 백오프가 실제로 잠들므로 횟수를 작게 잡는다. */
    private OpenAiScheduleParser parserWith(RestClient.Builder builder, int maxRetries) {
        AppProperties properties = new AppProperties(
                new AppProperties.UserDefaults("default", List.of()),
                new AppProperties.AiSetting("http://ai.test", "test-model",
                        Duration.ofSeconds(5), Duration.ofSeconds(30), maxRetries),
                new AppProperties.Security("test-key"));
        return new OpenAiScheduleParser(properties, mapper,
                builder.baseUrl("http://ai.test").build());
    }

    private CommandForAI command() {
        return new CommandForAI("내일 7시 운동",
                LocalDateTime.parse("2026-08-31T09:00:00"), List.of("육체"), "sk-test");
    }
}
