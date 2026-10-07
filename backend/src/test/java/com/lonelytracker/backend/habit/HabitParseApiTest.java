package com.lonelytracker.backend.habit;

import com.lonelytracker.backend.ai.AiUsage;
import com.lonelytracker.backend.ai.HabitParser;
import com.lonelytracker.backend.ai.ParsedHabit;
import com.lonelytracker.backend.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 습관 파싱 API.
 *
 * <p><b>실제 LLM 을 부르지 않는다.</b> {@link HabitParser} 를 가짜로 바꿔서
 * "AI 가 이런 답을 줬을 때 우리 서버가 어떻게 행동하는가" 를 검증한다.
 *
 * <p>여기서 보는 것은 셋이다 — 카테고리 이름을 id 로 맞추는지, 못 맞추면 되묻는지,
 * 그리고 사용자가 가진 카테고리 이름이 프롬프트의 후보로 실려 가는지.
 */
@AutoConfigureMockMvc
@Transactional
@Import(HabitParseApiTest.FakeHabitParserConfig.class)
class HabitParseApiTest extends IntegrationTest {

    private static final String PARSE = "/api/habits/parse";
    private static final String CREDENTIALS = "/api/users/me/ai-providers";
    private static final String BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/openai";

    @Autowired
    MockMvc mvc;

    @Autowired
    FakeHabitParser parser;

    @Autowired
    com.lonelytracker.backend.habit.repository.HabitCategoryRepository categoryRepository;

    @BeforeEach
    void setUp() throws Exception {
        parser.reset();
        // 파싱은 사용자 키가 있어야 동작한다. 서버 설정이 아니다
        mvc.perform(put(CREDENTIALS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"baseUrl\":\"" + BASE_URL
                                + "\",\"model\":\"gemini-2.5-flash\",\"apiKey\":\"sk-test-abc\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("AI 가 고른 카테고리 이름이 id 로 바뀌어 온다")
    void matchesTheCategoryByName() throws Exception {
        parser.willReturn(habit("팔굽혀펴기", "운동", "퇴근 후", "거실", "매트 깔기", true));

        mvc.perform(parse("퇴근 후 거실에서 팔굽혀펴기"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.habits.length()").value(1))
                .andExpect(jsonPath("$.habits[0].title").value("팔굽혀펴기"))
                .andExpect(jsonPath("$.habits[0].categoryId").value(categoryId("운동")))
                .andExpect(jsonPath("$.habits[0].atTime").value("퇴근 후"))
                .andExpect(jsonPath("$.habits[0].place").value("거실"))
                .andExpect(jsonPath("$.habits[0].twoMinuteAction").value("매트 깔기"))
                .andExpect(jsonPath("$.habits[0].suggestedAction").value(true))
                .andExpect(jsonPath("$.notice").doesNotExist());
    }

    @Test
    @DisplayName("사용자가 가진 카테고리 이름이 후보로 실려 간다")
    void sendsTheUserCategoriesAsCandidates() throws Exception {
        parser.willReturn(habit("명상", "마음챙김", null, null, null, false));

        mvc.perform(parse("명상하기")).andExpect(status().isOk());

        // V3 가 심어 둔 여섯. 차례까지 그대로 가야 프롬프트의 예시와 어긋나지 않는다
        assertThat(parser.lastCategories)
                .containsExactly("운동", "마음챙김", "부업", "예술", "학습", "인간관계");
    }

    @Test
    @DisplayName("없는 카테고리를 돌리면 비우고 고르라고 되묻는다")
    void asksWhenTheCategoryIsUnknown() throws Exception {
        parser.willReturn(habit("설거지", "살림", null, null, null, false));

        mvc.perform(parse("설거지하기"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.habits[0].categoryId").doesNotExist())
                .andExpect(jsonPath("$.habits[0].questions",
                        org.hamcrest.Matchers.hasItem("CATEGORY")));
    }

    @Test
    @DisplayName("이름의 대소문자와 앞뒤 공백은 무시해 맞춘다")
    void matchesLooselyOnSpacing() throws Exception {
        parser.willReturn(habit("달리기", "  운동 ", null, null, null, false));

        mvc.perform(parse("달리기"))
                .andExpect(jsonPath("$.habits[0].categoryId").value(categoryId("운동")));
    }

    @Test
    @DisplayName("2분 행동이 비면 제안 깃발도 내려간다")
    void dropsTheFlagWhenThereIsNoAction() throws Exception {
        // 깃발만 남으면 화면이 없는 값을 "제안"이라고 말한다
        parser.willReturn(habit("명상", "마음챙김", null, null, null, true));

        mvc.perform(parse("명상"))
                .andExpect(jsonPath("$.habits[0].twoMinuteAction").doesNotExist())
                .andExpect(jsonPath("$.habits[0].suggestedAction").value(false));
    }

    @Test
    @DisplayName("이름 없는 초안은 버린다")
    void dropsANamelessDraft() throws Exception {
        parser.willReturn(habit(" ", "운동", null, null, null, false));

        mvc.perform(parse("무언가"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.habits.length()").value(0))
                .andExpect(jsonPath("$.notice").value(
                        org.hamcrest.Matchers.containsString("직접 입력")));
    }

    @Test
    @DisplayName("문장이 비면 400이다")
    void refusesABlankSentence() throws Exception {
        mvc.perform(parse("  ")).andExpect(status().isBadRequest());
    }

    // --- 거들기 -----------------------------------------------------------

    private org.springframework.test.web.servlet.RequestBuilder parse(String text)
            throws Exception {
        return post(PARSE)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"" + text + "\"}");
    }

    /** V3 가 심어 둔 카테고리의 id. DB 가 정하므로 이름으로 찾는다 */
    private long categoryId(String name) {
        return categoryRepository.findAll().stream()
                .filter(c -> c.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("심어 둔 카테고리가 없다: " + name))
                .getId();
    }

    private static ParsedHabit habit(String title, String category, String atTime, String place,
            String action, boolean suggested) {
        return new ParsedHabit(title, category, atTime, place, action, suggested, List.of());
    }

    /** 정해진 답을 돌려주는 가짜. 실제 API 를 부르지 않는다 */
    static class FakeHabitParser implements HabitParser {

        private Function<String, List<ParsedHabit>> behavior;
        List<String> lastCategories;

        void reset() {
            behavior = null;
            lastCategories = null;
        }

        void willReturn(ParsedHabit... results) {
            List<ParsedHabit> all = List.of(results);
            this.behavior = text -> all;
        }

        @Override
        public HabitParseResult parse(HabitParseCommand command) {
            this.lastCategories = command.categories();
            return new HabitParseResult(behavior.apply(command.text()), new AiUsage(11, 22));
        }
    }

    @TestConfiguration
    static class FakeHabitParserConfig {

        @Bean
        @Primary
        FakeHabitParser fakeHabitParser() {
            return new FakeHabitParser();
        }
    }

}
