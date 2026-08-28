package com.lonelytracker.backend.ai;

import com.lonelytracker.backend.common.AppProperties;
import com.lonelytracker.backend.common.exception.AiParseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Responses API 응답 봉투에서 결과를 꺼내는 부분.
 * <p>
 * 실제 호출 없이 <b>봉투 모양만</b> 검증한다. 처음 붙였을 때
 * {@code output[0].content[0].text} 로 인덱스를 찍었다가 실패했다 —
 * 배열의 첫 항목이 {@code type: "reasoning"} 이고 실제 답은 그 뒤에 있었다.
 * 이런 건 가짜 파서로는 못 잡고 여기서만 잡힌다.
 */
class OpenAiResponseShapeTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final OpenAiScheduleParser parser = new OpenAiScheduleParser(properties(), mapper);

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

        assertThat(extract(envelope)).isEqualTo("{\"title\":\"운동\"}");
    }

    @Test
    @DisplayName("message 가 맨 앞이어도 찾아낸다")
    void findsMessageWhenFirst() {
        String envelope = """
                {
                  "output": [
                    { "type": "message", "role": "assistant",
                      "content": [ { "type": "output_text", "text": "결과" } ] }
                  ]
                }""";

        assertThat(extract(envelope)).isEqualTo("결과");
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
                        { "type": "output_text", "text": "진짜 결과" }
                      ] }
                  ]
                }""";

        assertThat(extract(envelope)).isEqualTo("진짜 결과");
    }

    @Test
    @DisplayName("message 가 없으면 어떤 항목이 왔는지 알려준다")
    void reportsItemTypesWhenMessageMissing() {
        String envelope = """
                { "output": [ { "type": "reasoning", "content": [] } ] }""";

        // 무엇이 왔는지 모르면 진단이 불가능하다
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

    /** private 메서드라 리플렉션으로 부른다. 봉투 해석만 떼어 보려는 목적이다. */
    private String extract(String envelope) {
        try {
            Method method = OpenAiScheduleParser.class
                    .getDeclaredMethod("extractOutputText", String.class);
            method.setAccessible(true);
            return (String) method.invoke(parser, envelope);
        } catch (ReflectiveOperationException e) {
            if (e.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException(e);
        }
    }

    private AppProperties properties() {
        return new AppProperties(
                new AppProperties.UserDefaults("default", List.of()),
                new AppProperties.AiSetting("http://localhost", "test-model",
                        Duration.ofSeconds(5), Duration.ofSeconds(30), 2),
                new AppProperties.Security("test-key"));
    }
}
