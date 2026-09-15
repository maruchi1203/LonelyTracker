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

/** 실제 호출 없이 Chat Completions 응답 봉투에서 결과를 꺼내는 부분만 검증한다. */
class ChatCompletionsShapeTest {

  private final ObjectMapper mapper = new ObjectMapper();
  private final AiScheduleParser parser =
      new AiScheduleParser(properties(), mapper, RestClient.create());

  @Test
  @DisplayName("content 안의 JSON 을 꺼낸다")
  void readsContentJson() {
    String envelope = """
        {
          "choices": [
            { "index": 0, "finish_reason": "stop",
              "message": { "role": "assistant", "content": "{\\"title\\":\\"운동\\"}" } }
          ]
        }""";

    assertThat(extract(envelope).path("title").asString()).isEqualTo("운동");
  }

  @Test
  @DisplayName("빈 choice 가 앞에 있어도 내용이 있는 것을 고른다")
  void skipsEmptyChoice() {
    String envelope = """
        {
          "choices": [
            { "index": 0, "message": { "role": "assistant", "content": "" } },
            { "index": 1, "message": { "role": "assistant", "content": "{\\"value\\":\\"결과\\"}" } }
          ]
        }""";

    assertThat(extract(envelope).path("value").asString()).isEqualTo("결과");
  }

  @Test
  @DisplayName("모델이 거절하면 그 이유를 알려준다")
  void refusalBecomesParseException() {
    String envelope = """
        {
          "choices": [
            { "index": 0, "finish_reason": "stop",
              "message": { "role": "assistant", "content": null, "refusal": "그건 할 수 없습니다" } }
          ]
        }""";

    assertThatThrownBy(() -> extract(envelope))
        .isInstanceOf(AiParseException.class)
        .hasMessageContaining("그건 할 수 없습니다");
  }

  @Test
  @DisplayName("내용이 없으면 종료 사유를 알려준다")
  void reportsFinishReasonWhenEmpty() {
    String envelope = """
        { "choices": [ { "index": 0, "finish_reason": "length",
            "message": { "role": "assistant", "content": "" } } ] }""";

    assertThatThrownBy(() -> extract(envelope))
        .isInstanceOf(AiParseException.class)
        .hasMessageContaining("length");
  }

  @Test
  @DisplayName("choices 가 아예 없어도 예외로 끝난다")
  void missingChoicesBecomesParseException() {
    assertThatThrownBy(() -> extract("{ \"id\": \"chatcmpl-1\" }"))
        .isInstanceOf(AiParseException.class);
  }

  @Test
  @DisplayName("JSON 이 아니면 400 계열 예외로 바꾼다")
  void brokenEnvelopeBecomesParseException() {
    assertThatThrownBy(() -> extract("이건 JSON 이 아니다"))
        .isInstanceOf(AiParseException.class);
  }

  @Test
  @DisplayName("content 가 JSON 이 아니면 예외로 바꾼다")
  void brokenContentBecomesParseException() {
    String envelope = """
        { "choices": [ { "message": { "role": "assistant", "content": "일정이 없네요" } } ] }""";

    assertThatThrownBy(() -> extract(envelope))
        .isInstanceOf(AiParseException.class);
  }

  @Test
  @DisplayName("사용량은 prompt_tokens 와 completion_tokens 에서 읽는다")
  void readsUsage() {
    AiUsage usage = new ChatCompletionsProtocol().usageOf(mapper.readTree("""
        { "usage": { "prompt_tokens": 120, "completion_tokens": 45, "total_tokens": 165 } }"""));

    assertThat(usage).isEqualTo(new AiUsage(120, 45));
  }

  @Test
  @DisplayName("completion_tokens 밖에 있는 생각 토큰은 total_tokens 에서 되찾아 출력에 넣는다")
  void countsThinkingOutsideCompletion() {
    AiUsage usage = new ChatCompletionsProtocol().usageOf(mapper.readTree("""
        { "usage": { "prompt_tokens": 758, "completion_tokens": 102, "total_tokens": 1725 } }"""));

    assertThat(usage).isEqualTo(new AiUsage(758, 967));
  }

  @Test
  @DisplayName("total_tokens 가 없으면 completion_tokens 를 그대로 쓴다")
  void missingTotalKeepsCompletion() {
    AiUsage usage = new ChatCompletionsProtocol().usageOf(mapper.readTree("""
        { "usage": { "prompt_tokens": 120, "completion_tokens": 45 } }"""));

    assertThat(usage).isEqualTo(new AiUsage(120, 45));
  }

  @Test
  @DisplayName("사용량이 없는 응답이면 0 으로 본다")
  void missingUsageIsZero() {
    assertThat(new ChatCompletionsProtocol().usageOf(mapper.readTree("{}")))
        .isEqualTo(AiUsage.NONE);
  }

  private JsonNode extract(String envelope) {
    return parser.extractOutput(new ChatCompletionsProtocol(), envelope);
  }

  private AppProperties properties() {
    return new AppProperties(
        new AppProperties.UserDefaults("default"),
        new AppProperties.AiSetting("http://localhost", "test-model", null,
            Duration.ofSeconds(5), Duration.ofSeconds(30), 2),
        new AppProperties.Security("test-key"));
  }
}
