package com.lonelytracker.backend.schedule;

import com.lonelytracker.backend.ai.CommandForAI;
import com.lonelytracker.backend.ai.ParseQuestion;
import com.lonelytracker.backend.ai.ParsedRecurringSchedule;
import com.lonelytracker.backend.ai.ParsedSchedule;
import com.lonelytracker.backend.ai.ScheduleParser;
import com.lonelytracker.backend.common.exception.AiParseException;
import com.lonelytracker.backend.common.exception.AiUnavailableException;
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

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 자연어 파싱 API.
 * <p>
 * <b>실제 LLM 을 부르지 않는다.</b> ScheduleParser 인터페이스를 가짜로 바꿔서
 * "AI 가 이런 답을 줬을 때 우리 서버가 어떻게 행동하는가" 를 검증한다.
 * AI 가 똑똑한지를 검증하는 것이 아니다.
 * <p>
 * 빠르고, 비용이 0이고, 네트워크에 안 묶인다. 인터페이스를 둔 진짜 이유다.
 */
@AutoConfigureMockMvc
@Transactional
@Import(ScheduleParseApiTest.FakeParserConfig.class)
class ScheduleParseApiTest extends IntegrationTest {

    private static final String PARSE = "/api/schedules/parse";

    @Autowired
    MockMvc mvc;

    @Autowired
    FakeParser parser;

    @BeforeEach
    void resetParser() throws Exception {
        parser.reset();
        registerKey("sk-test-abcdefgh");
    }

    /** 파싱은 사용자 키가 있어야 동작한다. 서버 설정이 아니다. */
    private void registerKey(String apiKey) throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .put("/api/users/me/openai-key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"apiKey\":\"" + apiKey + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("파싱 결과를 초안으로 돌려주고 저장하지는 않는다")
    void returnsDraftWithoutSaving() throws Exception {
        mvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"육체\"}")).andExpect(status().isCreated());
        parser.willReturn(draft("헬스장 운동", "2026-09-08T15:00:00", "헬스장", "육체"));

        mvc.perform(post(PARSE).contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"내일 3시 헬스장에서 운동\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("헬스장 운동"))
                .andExpect(jsonPath("$.startAt").value("2026-09-08T15:00:00"))
                .andExpect(jsonPath("$.place").value("헬스장"))
                .andExpect(jsonPath("$.category").value("육체"));

        // 저장되지 않았다 - 목록이 비어 있어야 한다
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/schedules")
                .param("from", "2026-09-01T00:00:00")
                .param("to", "2026-09-30T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").doesNotExist());
    }

    @Test
    @DisplayName("현재 시각이 파서에 전달된다")
    void passesCurrentTimeToParser() throws Exception {
        parser.willReturn(draft("회의", "2026-09-08T15:00:00", null, null));

        mvc.perform(post(PARSE).contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"내일 3시 회의\"}")).andExpect(status().isOk());

        // "내일" 을 해석하려면 오늘이 며칠인지 알아야 한다. 없으면 엉뚱한 날짜가 나온다
        assertThat(parser.lastNow).isNotNull();
        assertThat(parser.lastNow.toLocalDate()).isEqualTo(java.time.LocalDate.now());
    }

    @Test
    @DisplayName("사용자의 카테고리 목록이 파서에 전달된다")
    void passesCategoriesToParser() throws Exception {
        parser.willReturn(draft("운동", "2026-09-08T15:00:00", null, null));

        mvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"육체\"}")).andExpect(status().isCreated());

        mvc.perform(post(PARSE).contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"운동\"}")).andExpect(status().isOk());

        // 목록을 안 주면 모델이 없는 분류를 만들어 낸다
        assertThat(parser.lastCategories).contains("육체");
    }

    @Test
    @DisplayName("목록에 없는 분류를 지어내면 버린다")
    void dropsInventedCategory() throws Exception {
        mvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"육체\"}")).andExpect(status().isCreated());
        parser.willReturn(draft("운동", "2026-09-08T15:00:00", null, "존재하지않는분류"));

        mvc.perform(post(PARSE).contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"운동\"}"))
                .andExpect(status().isOk())
                // non_null 직렬화라 버려진 값은 필드째 빠진다
                .andExpect(jsonPath("$.category").doesNotExist());
    }

    @Test
    @DisplayName("반복 규칙도 초안에 실린다")
    void returnsRecurrence() throws Exception {
        parser.willReturn(new ParsedSchedule(
                "운동", LocalDateTime.parse("2026-09-07T07:00:00"), null, false, "육체", "헬스장",
                new ParsedRecurringSchedule(RecurrenceFreq.WEEKLY,
                        EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY), null),
                List.of()));

        mvc.perform(post(PARSE).contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"매주 월수금 아침 7시 헬스장에서 운동\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recurrence.freq").value("WEEKLY"))
                .andExpect(jsonPath("$.recurrence.byWeekday.length()").value(3));
    }

    @Test
    @DisplayName("못 채운 칸은 질문 ID로 돌아온다")
    void returnsQuestionsForMissingFields() throws Exception {
        parser.willReturn(new ParsedSchedule(
                "회의", null, null, false, null, null, null,
                List.of(ParseQuestion.DATE, ParseQuestion.START_TIME, ParseQuestion.PLACE)));

        mvc.perform(post(PARSE).contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"회의\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("회의"))
                // 모르는 값은 지어내지 않고 null 로 둔다
                .andExpect(jsonPath("$.startAt").doesNotExist())
                .andExpect(jsonPath("$.questions.length()").value(3))
                .andExpect(jsonPath("$.questions[0]").value("DATE"));
    }

    @Test
    @DisplayName("제목이 비면 400을 반환한다")
    void rejectsEmptyTitle() throws Exception {
        parser.willReturn(new ParsedSchedule(
                null, LocalDateTime.parse("2026-09-08T15:00:00"), null, false,
                null, null, null, List.of()));

        mvc.perform(post(PARSE).contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"ㅁㄴㅇㄹ\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("종료가 시작보다 이르면 400을 반환한다")
    void rejectsInvertedPeriod() throws Exception {
        parser.willReturn(new ParsedSchedule(
                "거꾸로",
                LocalDateTime.parse("2026-09-08T15:00:00"),
                LocalDateTime.parse("2026-09-08T14:00:00"),
                false, null, null, null, List.of()));

        mvc.perform(post(PARSE).contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"거꾸로\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("사용자의 API 키가 파서에 전달된다")
    void passesUserApiKeyToParser() throws Exception {
        parser.willReturn(draft("운동", "2026-09-08T15:00:00", null, null));

        mvc.perform(post(PARSE).contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"운동\"}")).andExpect(status().isOk());

        // 서버 설정이 아니라 이 사용자의 키다
        assertThat(parser.lastApiKey).isEqualTo("sk-test-abcdefgh");
    }

    @Test
    @DisplayName("키를 등록하지 않은 사용자는 503을 받는다")
    void returnsServiceUnavailableWithoutKey() throws Exception {
        registerKey(""); // 등록 해제

        // 사용자 잘못이 아니므로 4xx 가 아니다. 나머지 기능은 그대로 쓴다
        mvc.perform(post(PARSE).contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"내일 3시 회의\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("AI 호출이 실패하면 503을 반환한다")
    void returnsServiceUnavailableOnFailure() throws Exception {
        parser.willThrow(new AiUnavailableException("AI 응답을 받지 못했습니다"));

        mvc.perform(post(PARSE).contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"내일 3시 회의\"}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("AI 응답이 깨졌으면 500이 아니라 400을 반환한다")
    void brokenResponseBecomesBadRequest() throws Exception {
        parser.willThrow(new AiParseException("AI 가 만든 JSON 을 읽지 못했습니다"));

        mvc.perform(post(PARSE).contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"내일 3시 회의\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("text가 비면 400을 반환한다")
    void rejectsBlankText() throws Exception {
        mvc.perform(post(PARSE).contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("text가 500자를 넘으면 400을 반환한다")
    void rejectsTooLongText() throws Exception {
        // 상한이 없으면 토큰 비용이 입력 길이를 그대로 따라간다
        String tooLong = "가".repeat(501);

        mvc.perform(post(PARSE).contentType(MediaType.APPLICATION_JSON)
                .content("{\"text\":\"" + tooLong + "\"}"))
                .andExpect(status().isBadRequest());
    }

    // --- 가짜 파서 ---------------------------------------------------------

    private ParsedSchedule draft(String title, String startAt, String place, String category) {
        return new ParsedSchedule(title, LocalDateTime.parse(startAt), null, false,
                category, place, null, List.of());
    }

    /** 정해진 답을 돌려주는 가짜. 실제 API 를 부르지 않는다. */
    static class FakeParser implements ScheduleParser {

        private Function<String, ParsedSchedule> behavior;
        LocalDateTime lastNow;
        List<String> lastCategories;
        String lastApiKey;

        void reset() {
            behavior = null;
            lastNow = null;
            lastCategories = null;
            lastApiKey = null;
        }

        void willReturn(ParsedSchedule result) {
            this.behavior = text -> result;
        }

        void willThrow(RuntimeException e) {
            this.behavior = text -> {
                throw e;
            };
        }

        @Override
        public ParsedSchedule parse(CommandForAI command) {
            this.lastNow = command.now();
            this.lastCategories = command.categories();
            this.lastApiKey = command.apiKey();
            return behavior.apply(command.text());
        }
    }

    @TestConfiguration
    static class FakeParserConfig {

        @Bean
        @Primary
        FakeParser fakeParser() {
            return new FakeParser();
        }
    }
}
