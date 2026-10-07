package com.lonelytracker.backend.ai;

import com.lonelytracker.backend.common.exception.AiParseException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 자연어를 습관 초안으로 바꾼다.
 *
 * <p>묻는 일은 {@link AiCaller} 가 맡고, 여기 남은 것은 습관을 위한 프롬프트와 해석이다.
 * 일정 쪽과 나눠 둔 까닭은 받을 것과 되물을 것이 다르기 때문이다 —
 * 습관에는 시각 대신 신호가, 태그 대신 카테고리가, 그리고 2분 행동이 있다.
 */
@Component
public class AiHabitParser implements HabitParser {

    private final AiCaller caller;

    public AiHabitParser(AiCaller caller) {
        this.caller = caller;
    }

    @Override
    public HabitParseResult parse(HabitParseCommand command) {
        AiCaller.Answer answer = caller.ask(
                command.baseUrl(), command.model(), command.apiKey(),
                new AiCaller.Prompt(
                        systemPrompt(command.categories()),
                        command.text(),
                        new ParsedHabitSchema(command.categories())));

        return new HabitParseResult(toParsedList(answer.output()), answer.usage());
    }

    // --- 요청 조립 --------------------------------------------------------

    /**
     * 규칙과 예시를 담은 system 메시지.
     * 칸별 규칙은 {@link ParsedHabitSchema} 의 description 이 들고 있다
     */
    private String systemPrompt(List<String> categories) {
        String list = categories.isEmpty() ? "(없음)" : String.join(", ", categories);

        return """
                너는 한국어 문장을 "기르려는 습관"으로 바꾸는 도구다.
                일정이 아니라 되풀이할 행동을 뽑는다. 칸별 규칙은 스키마의 description 을 따른다.

                - 습관은 날짜를 갖지 않는다. "내일", "3시까지" 같은 말이 있어도 날짜로 적지 않는다.
                  때를 가리키는 말은 atTime 에 글자 그대로 담는다.
                - categoryName 은 아래 후보 중 하나만 고른다. 새로 지어내지 않는다.
                  어느 것도 가깝지 않으면 비우고 questions 에 CATEGORY 를 넣는다.
                - twoMinuteAction 은 문장에 없으면 지어 적고 suggestedAction 을 true 로 둔다.
                  시작을 막는 가장 큰 장벽은 첫 동작이라, 비워 두는 것보다 고쳐 쓸 것을 주는 쪽이 낫다.
                  다만 지어낸 것임을 반드시 true 로 밝힌다.
                - atTime 과 place 는 지어내지 않는다. 없으면 비우고 questions 에 CUE_TIME·PLACE 를 넣는다.
                  이 둘은 사람의 하루에 달린 것이라 지어내면 틀린다.
                - 문장에 서로 다른 습관이 여럿이면 habits 배열에 하나씩 나눠 담는다.

                예시 — 카테고리 후보가 [운동, 학습, 마음챙김] 일 때:
                "퇴근 후 거실에서 팔굽혀펴기"
                  title=팔굽혀펴기 categoryName=운동 atTime=퇴근 후 place=거실
                  twoMinuteAction=매트 깔기 suggestedAction=true questions=[]
                "매일 아침 7시에 영어 단어 30개 외우기, 자기 전엔 명상"
                  habits 에 두 칸 —
                  title=영어 단어 외우기 categoryName=학습 atTime=아침 7시
                  twoMinuteAction=단어장 펴기 suggestedAction=true questions=["PLACE"] 와
                  title=명상 categoryName=마음챙김 atTime=자기 전
                  twoMinuteAction=방석에 앉기 suggestedAction=true questions=["PLACE"]
                "운동하기"
                  title=운동 categoryName=운동 atTime=null place=null
                  twoMinuteAction=운동복 갈아입기 suggestedAction=true
                  questions=["CUE_TIME","PLACE"]
                "열심히 살기"
                  title=열심히 살기 questions=["TOO_VAGUE"] — 무엇을 할지 모르면 쪼개도록 되묻는다

                카테고리 후보: %s — 이 중에서만 고른다.
                """
                .formatted(list);
    }

    // --- 응답 해석 --------------------------------------------------------

    /**
     * 봉투 안의 배열을 초안 목록으로 바꾼다.
     * 하나도 못 읽으면 문장 자체를 습관으로 볼 수 없었던 것이다
     */
    private List<ParsedHabit> toParsedList(JsonNode root) {
        JsonNode habits = root.path("habits");
        if (!habits.isArray() || habits.isEmpty()) {
            throw new AiParseException(
                    "AI 가 습관을 하나도 읽지 못했습니다. 응답: " + AiCaller.hint(root.toString()));
        }

        List<ParsedHabit> parsed = new ArrayList<>();
        for (JsonNode node : habits) {
            if (parsed.size() >= ParsedHabitSchema.MAX_HABITS) {
                break;
            }
            parsed.add(toParsed(node));
        }
        return parsed;
    }

    private ParsedHabit toParsed(JsonNode node) {
        // 이름을 못 채우면 최소한의 습관을 형성할 수 없다
        if (textOrNull(node, "title") == null) {
            throw new AiParseException(
                    "AI 가 습관 이름을 채우지 못했습니다. 응답: " + AiCaller.hint(node.toString()));
        }

        String action = textOrNull(node, "twoMinuteAction");

        return new ParsedHabit(
                textOrNull(node, "title"),
                textOrNull(node, "categoryName"),
                textOrNull(node, "atTime"),
                textOrNull(node, "place"),
                action,
                node.path("suggestedAction").asBoolean(false),
                questionsOf(node.path("questions")));
    }

    /** 되물음 ID 목록. 모르는 ID 는 버린다 */
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

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asString();
        return text.isBlank() ? null : text;
    }
}
