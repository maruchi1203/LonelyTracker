package com.lonelytracker.backend.ai;

import com.lonelytracker.backend.common.exception.AiParseException;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OpenAI 호환 Chat Completions.
 * OpenAI·Gemini·Groq 등 대부분의 제공자가 이 규약을 낸다
 */
class ChatCompletionsProtocol implements AiProtocol {

    @Override
    public String path() {
        return "/chat/completions";
    }

    @Override
    public Map<String, String> authHeaders(String apiKey) {
        return Map.of("Authorization", "Bearer " + apiKey);
    }

    @Override
    public Map<String, Object> body(String model, String systemPrompt, String userText) {
        return Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userText)),
                "response_format", Map.of(
                        "type", "json_schema",
                        "json_schema", Map.of(
                                "name", "parsed_schedules",
                                "strict", true,
                                "schema", ParsedScheduleSchema.getRoot())));
    }

    @Override
    public String resultTextOf(JsonNode envelope) {
        for (JsonNode choice : envelope.path("choices")) {
            JsonNode message = choice.path("message");

            // 모델이 거절하면 그 이유가 결과 대신 온다
            String refusal = message.path("refusal").asString("");
            if (!refusal.isBlank()) {
                throw new AiParseException("AI 가 요청을 거절했습니다: " + refusal);
            }

            String text = message.path("content").asString("");
            if (!text.isBlank()) {
                return text;
            }
        }

        throw new AiParseException(
                "AI 응답에서 결과를 찾지 못했습니다. 종료 사유: " + finishReasons(envelope));
    }

    /** 응답이 왜 비었는지 알려 줄 단서. 길이 초과면 length 가 온다 */
    private static List<String> finishReasons(JsonNode envelope) {
        List<String> reasons = new ArrayList<>();
        envelope.path("choices").forEach(c -> reasons.add(c.path("finish_reason").asString("?")));
        return reasons;
    }
}
