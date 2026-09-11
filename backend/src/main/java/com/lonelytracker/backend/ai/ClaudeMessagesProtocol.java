package com.lonelytracker.backend.ai;

import com.lonelytracker.backend.common.exception.AiParseException;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Claude 네이티브 Messages API.
 * 호환 계층은 strict 를 무시하지만 이쪽은 문법 제약 샘플링으로 스키마를 보장한다.
 *
 * <p>규칙은 message 가 아니라 top-level system 에 싣고, 스키마는 null 유니온을 못 써
 * 없을 수 있는 칸을 required 에서 뺀 뿌리를 쓴다.
 */
class ClaudeMessagesProtocol implements AiProtocol {

    /** 이 API 는 상한이 필수다. 일정 10개의 JSON 이 들어갈 만큼만 준다 */
    private static final int MAX_TOKENS = 4096;

    /** 요청 본문 규격의 판. 날짜형 상수라 올릴 때까지 이 값으로 고정된다 */
    private static final String VERSION = "2023-06-01";

    @Override
    public String path() {
        return "/messages";
    }

    @Override
    public Map<String, String> authHeaders(String apiKey) {
        return Map.of("x-api-key", apiKey, "anthropic-version", VERSION);
    }

    @Override
    public Map<String, Object> body(String model, String systemPrompt, String userText) {
        return Map.of(
                "model", model,
                "max_tokens", MAX_TOKENS,
                "system", systemPrompt,
                "messages", List.of(Map.of("role", "user", "content", userText)),
                "output_config", Map.of("format", Map.of(
                        "type", "json_schema",
                        "schema", ParsedScheduleSchema.getRootWithOptionalFields())));
    }

    @Override
    public String resultTextOf(JsonNode envelope) {
        for (JsonNode block : envelope.path("content")) {
            if (!"text".equals(block.path("type").asString(""))) {
                continue;
            }
            String text = block.path("text").asString("");
            if (!text.isBlank()) {
                return text;
            }
        }

        throw new AiParseException(
                "AI 응답에서 결과를 찾지 못했습니다. 종료 사유: "
                        + envelope.path("stop_reason").asString("?")
                        + ", 블록: " + blockTypes(envelope));
    }

    /** 텍스트가 없을 때 무엇이 왔는지 알려 줄 단서 */
    private static List<String> blockTypes(JsonNode envelope) {
        List<String> types = new ArrayList<>();
        envelope.path("content").forEach(b -> types.add(b.path("type").asString("?")));
        return types;
    }
}
