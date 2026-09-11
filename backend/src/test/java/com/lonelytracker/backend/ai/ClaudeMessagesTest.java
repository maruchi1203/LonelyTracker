package com.lonelytracker.backend.ai;

import com.lonelytracker.backend.common.AppProperties;
import com.lonelytracker.backend.common.exception.AiParseException;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Claude 네이티브 Messages 규약을 가짜 HTTP 서버에 붙여 검증한다.
 * 호환 계층은 strict 를 무시하므로 Claude 는 이 길로만 부른다.
 */
class ClaudeMessagesTest {

  private static final String BASE = "https://api.anthropic.com/v1";
  private static final String ENDPOINT = BASE + "/messages";

  /** 성공 응답의 최소 형태. 결과 JSON 은 text 블록에 실려 온다 */
  private static final String OK_BODY = """
      { "type": "message", "stop_reason": "end_turn", "content": [
          { "type": "text",
            "text": "{\\"schedules\\":[{\\"title\\":\\"운동\\",\\"startAt\\":\\"2026-09-01T07:00:00\\"}]}" } ] }""";

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  @DisplayName("anthropic 주소면 네이티브 규약을 고른다")
  void picksNativeProtocolForAnthropic() {
    assertThat(AiScheduleParser.protocolFor(BASE)).isInstanceOf(ClaudeMessagesProtocol.class);
    assertThat(AiScheduleParser.protocolFor("https://api.openai.com/v1"))
        .isInstanceOf(ChatCompletionsProtocol.class);
    assertThat(AiScheduleParser.protocolFor(null)).isInstanceOf(ChatCompletionsProtocol.class);
  }

  @Test
  @DisplayName("x-api-key 와 버전 헤더를 싣고 /messages 로 보낸다")
  void sendsNativeHeaders() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    server.expect(requestTo(ENDPOINT))
        .andExpect(header("x-api-key", "sk-test"))
        .andExpect(header("anthropic-version", "2023-06-01"))
        .andExpect(jsonPath("$.max_tokens").exists())
        .andExpect(jsonPath("$.system").exists())
        .andExpect(jsonPath("$.output_config.format.type").value("json_schema"))
        .andRespond(withSuccess(OK_BODY, MediaType.APPLICATION_JSON));

    assertThat(parserWith(builder).parse(command()).get(0).title()).isEqualTo("운동");
    server.verify();
  }

  @Test
  @DisplayName("스키마에 null 유니온과 배열 상한을 쓰지 않는다")
  void schemaAvoidsUnsupportedKeywords() {
    String schema = mapper.writeValueAsString(
        ParsedScheduleSchema.getRootWithOptionalFields());

    assertThat(schema).doesNotContain("\"null\"");
    assertThat(schema).doesNotContain("maxItems");
  }

  @Test
  @DisplayName("없을 수 있는 칸은 required 에서 빠진다")
  void optionalFieldsAreNotRequired() {
    @SuppressWarnings("unchecked")
    Map<String, Object> item = (Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) ParsedScheduleSchema
        .getRootWithOptionalFields().get("properties")).get("schedules")).get("items");

    assertThat((List<Object>) item.get("required"))
        .doesNotContain("title", "startAt", "recurrence")
        .contains("allDay", "tags");
  }

  @Test
  @DisplayName("text 블록이 없으면 종료 사유를 알려준다")
  void reportsStopReasonWhenNoText() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    server.expect(requestTo(ENDPOINT)).andRespond(withSuccess("""
        { "stop_reason": "max_tokens", "content": [ { "type": "thinking" } ] }""",
        MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> parserWith(builder).parse(command()))
        .isInstanceOf(AiParseException.class)
        .hasMessageContaining("max_tokens");

    server.verify();
  }

  @Test
  @DisplayName("거절 사유를 그대로 실어 준다")
  void carriesRejectionReason() {
    RestClient.Builder builder = RestClient.builder();
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

    server.expect(requestTo(ENDPOINT))
        .andRespond(withStatus(HttpStatus.BAD_REQUEST)
            .body("""
                { "error": { "type": "invalid_request_error", "message": "max_tokens: required" } }""")
            .contentType(MediaType.APPLICATION_JSON));

    assertThatThrownBy(() -> parserWith(builder).parse(command()))
        .isInstanceOf(AiParseException.class)
        .hasMessageContaining("max_tokens: required");

    server.verify();
  }

  // --- 헬퍼 -------------------------------------------------------------

  private AiScheduleParser parserWith(RestClient.Builder builder) {
    AppProperties properties = new AppProperties(
        new AppProperties.UserDefaults("default"),
        new AppProperties.AiSetting(BASE, "claude-opus-5",
            Duration.ofSeconds(5), Duration.ofSeconds(30), 0),
        new AppProperties.Security("test-key"));
    return new AiScheduleParser(properties, mapper, builder.baseUrl(BASE).build());
  }

  private AiParseCommand command() {
    return new AiParseCommand("내일 7시 운동",
        LocalDateTime.parse("2026-08-31T09:00:00"), List.of("육체"), "sk-test");
  }
}
