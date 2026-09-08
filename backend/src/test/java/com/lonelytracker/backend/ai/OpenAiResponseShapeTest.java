package com.lonelytracker.backend.ai;

import com.lonelytracker.backend.common.AppProperties;
import com.lonelytracker.backend.common.exception.AiParseException;
import org.junit.jupiter.api.DisplayName;
import org.springframework.web.client.RestClient;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 실제 호출 없이 Responses API 응답 봉투에서 결과를 꺼내는 부분만 검증한다. */
class OpenAiResponseShapeTest {

  private final ObjectMapper mapper = new ObjectMapper();
  private final OpenAiScheduleParser parser = new OpenAiScheduleParser(properties(), mapper, RestClient.create());

  @Test
  @DisplayName("reasoning 항목이 앞에 있어도 message 를 찾아낸다")
  void findsMessageAfterReasoning() {
    String envelope = """
        {
          "output": [
            { "id": "rs_1", "type": "reasoning", "content": [], "summary": [] },
            { "id": "msg_1", "type": "message", "status": "completed", "role": "assistant",
              "content": [ { "type": "output_text", "text": "{\\"title\\":\\"운동\\"}" } ] }
          ]
        }""";

    assertThat(extract(envelope).path("title").asString()).isEqualTo("운동");
  }

  @Test
  @DisplayName("message 가 맨 앞이어도 찾아낸다")
  void findsMessageWhenFirst() {
    String envelope = """
        {
          "output": [
            { "type": "message", "role": "assistant",
              "content": [ { "type": "output_text", "text": "{\\"value\\":\\"결과\\"}" } ] }
          ]
        }""";

    assertThat(extract(envelope).path("value").asString()).isEqualTo("결과");
  }

  @Test
  @DisplayName("content 안에 다른 타입이 섞여 있어도 output_text 를 고른다")
  void picksOutputTextAmongOthers() {
    String envelope = """
        {
          "output": [
            { "type": "message", "role": "assistant",
              "content": [
                { "type": "refusal", "refusal": "못 하겠음" },
                { "type": "output_text", "text": "{\\"value\\":\\"진짜 결과\\"}" }
              ] }
          ]
        }""";

    assertThat(extract(envelope).path("value").asString()).isEqualTo("진짜 결과");
  }

  @Test
  @DisplayName("message 가 없으면 어떤 항목이 왔는지 알려준다")
  void reportsItemTypesWhenMessageMissing() {
    String envelope = """
        { "output": [ { "type": "reasoning", "content": [] } ] }""";

    assertThatThrownBy(() -> extract(envelope))
        .isInstanceOf(AiParseException.class)
        .hasMessageContaining("reasoning");
  }

  @Test
  @DisplayName("JSON 이 아니면 400 계열 예외로 바꾼다")
  void brokenEnvelopeBecomesParseException() {
    assertThatThrownBy(() -> extract("이건 JSON 이 아니다"))
        .isInstanceOf(AiParseException.class);
  }

  private JsonNode extract(String envelope) {
    return parser.extractOutput(envelope);
  }

  private AppProperties properties() {
    return new AppProperties(
        new AppProperties.UserDefaults("default"),
        new AppProperties.AiSetting("http://localhost", "test-model",
            Duration.ofSeconds(5), Duration.ofSeconds(30), 2),
        new AppProperties.Security("test-key"));
  }
}
