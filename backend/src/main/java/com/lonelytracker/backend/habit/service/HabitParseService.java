package com.lonelytracker.backend.habit.service;

import com.lonelytracker.backend.ai.AiTarget;
import com.lonelytracker.backend.ai.AiTargetResolver;
import com.lonelytracker.backend.ai.AiUsageRecorder;
import com.lonelytracker.backend.ai.HabitParser;
import com.lonelytracker.backend.ai.ParseQuestion;
import com.lonelytracker.backend.ai.ParsedHabit;
import com.lonelytracker.backend.common.exception.AiLimitExceededException;
import com.lonelytracker.backend.habit.dto.HabitDraftResponse;
import com.lonelytracker.backend.habit.dto.HabitParseResponse;
import com.lonelytracker.backend.habit.entity.HabitCategoryEntity;
import com.lonelytracker.backend.habit.repository.HabitCategoryRepository;
import com.lonelytracker.backend.user.service.UserProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 자연어를 습관 초안으로 바꾼다. 초안은 저장하지 않는다.
 *
 * <p>클래스에 {@code @Transactional} 을 두지 않아 수 초 걸리는 LLM 호출이 DB 커넥션을
 * 잡지 않는다. 카테고리를 읽는 짧은 트랜잭션만 호출 앞에서 열고 닫는다.
 */
@Service
@RequiredArgsConstructor
public class HabitParseService {

    private static final Logger log = LoggerFactory.getLogger(HabitParseService.class);

    private final HabitParser habitParser;
    private final HabitCategoryRepository categoryRepository;
    private final UserProvider currentUserProvider;
    private final AiTargetResolver targetResolver;
    private final AiUsageRecorder usageRecorder;

    /**
     * 문장 하나를 초안 목록으로 바꾼다.
     *
     * @return 읽어낸 초안들. 하나도 못 읽었으면 빈 목록과 안내 문구
     */
    public HabitParseResponse parse(String text) {
        AiTarget target;
        try {
            target = targetResolver.resolve();
        } catch (AiLimitExceededException e) {
            // 사용자가 정해 둔 한도다. 오류가 아니라 안내로 내보낸다
            return HabitParseResponse.notice(e.getMessage());
        }

        // 짧은 트랜잭션. 호출 전에 닫힌다
        Map<String, Long> byName = categoriesByName();

        // 트랜잭션 밖에서 호출
        HabitParser.HabitParseResult result = habitParser.parse(
                new HabitParser.HabitParseCommand(text, List.copyOf(byName.keySet()),
                        target.baseUrl(), target.model(), target.apiKey()));

        // 결과를 받았으면 토큰은 쓴 것이다. 기록이 실패해도 초안은 돌려준다
        try {
            usageRecorder.record(target.baseUrl(), target.model(), result.usage());
        } catch (RuntimeException e) {
            log.warn("AI 사용량을 기록하지 못함", e);
        }

        // LLM 응답을 사용자 입력과 같은 등급으로 검증한다.
        // 하나가 어긋났다고 나머지까지 버리지 않는다
        List<HabitDraftResponse> usable = result.habits().stream()
                .filter(HabitParseService::isUsable)
                .map(parsed -> toDraft(parsed, byName))
                .toList();

        // 부르는 데 성공했고 읽을 것이 없었을 뿐이라 오류로 내보내지 않는다
        if (usable.isEmpty()) {
            return HabitParseResponse.notice("습관으로 읽을 수 없는 문장입니다. 직접 입력해 주세요");
        }
        return HabitParseResponse.of(usable);
    }

    /**
     * 이름으로 카테고리를 찾는 지도.
     *
     * <p>차례를 지키는 지도를 쓰는 까닭은 그 순서가 그대로 프롬프트의 후보 목록이 되어서다.
     *
     * <p>{@code @Transactional} 을 붙이지 않는다. 같은 빈 안에서 부르면 프록시를 타지 않아
     * 붙여도 듣지 않는다. 질의가 하나뿐이라 그 질의가 제 트랜잭션을 열고 닫으면 충분하다.
     */
    private Map<String, Long> categoriesByName() {
        Map<String, Long> byName = new LinkedHashMap<>();
        for (HabitCategoryEntity category : categoryRepository
                .findAllOf(currentUserProvider.get().getId())) {
            byName.put(category.getName(), category.getId());
        }
        return byName;
    }

    /** 이름이 없으면 습관을 만들 수 없다. 나머지 빈 칸은 잘못이 아니라 되물음의 대상이다 */
    private static boolean isUsable(ParsedHabit parsed) {
        return parsed.title() != null && !parsed.title().isBlank();
    }

    /**
     * AI 가 고른 이름을 id 로 바꾼다.
     *
     * <p>스키마의 enum 으로 후보를 박아 두므로 대개 그대로 맞는다. 그래도 한 번 더 보는
     * 까닭은 strict 를 무시하는 제공자가 있어서다. 못 맞추면 비워 두고 고르라고 되묻는다
     */
    private static HabitDraftResponse toDraft(ParsedHabit parsed, Map<String, Long> byName) {
        Long id = matchCategory(parsed.categoryName(), byName);
        if (id != null) {
            return HabitDraftResponse.of(parsed, id);
        }

        List<ParseQuestion> asking = parsed.questions().contains(ParseQuestion.CATEGORY)
                ? parsed.questions()
                : append(parsed.questions(), ParseQuestion.CATEGORY);

        return new HabitDraftResponse(parsed.title(), null, parsed.atTime(), parsed.place(),
                parsed.twoMinuteAction(), parsed.suggestedAction(), asking);
    }

    /** 이름 그대로 찾고, 없으면 대소문자와 앞뒤 공백만 무시해 한 번 더 본다 */
    private static Long matchCategory(String name, Map<String, Long> byName) {
        if (name == null || name.isBlank()) {
            return null;
        }

        Long exact = byName.get(name);
        if (exact != null) {
            return exact;
        }

        String loose = name.strip().toLowerCase(Locale.ROOT);
        return byName.entrySet().stream()
                .filter(e -> e.getKey().strip().toLowerCase(Locale.ROOT).equals(loose))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    private static List<ParseQuestion> append(List<ParseQuestion> questions, ParseQuestion one) {
        List<ParseQuestion> all = new java.util.ArrayList<>(questions);
        all.add(one);
        return List.copyOf(all);
    }
}
