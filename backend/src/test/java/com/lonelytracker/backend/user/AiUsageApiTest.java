package com.lonelytracker.backend.user;

import com.lonelytracker.backend.ai.AiUsage;
import com.lonelytracker.backend.support.IntegrationTest;
import com.lonelytracker.backend.user.repository.AiUsageRepository;
import com.lonelytracker.backend.user.service.AiUsageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AI 사용량.
 * <p>
 * 제공자의 사용량 API 는 관리자 키가 필요하고 Gemini 에는 아예 없어서, 응답에 실려 온 토큰을 직접 적는다.
 * 기간 경계는 앱과 같은 시계(JVM)로 만든 시각을 넣어 검증한다. DB 의 now() 는 컨테이너 시간대라 어긋날 수 있다.
 */
@AutoConfigureMockMvc
@Transactional
class AiUsageApiTest extends IntegrationTest {

    private static final String PATH = "/api/users/me/ai-usage";
    private static final String OPENAI = "https://api.openai.com/v1";
    private static final String GEMINI = "https://generativelanguage.googleapis.com/v1beta/openai";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    AiUsageService usageService;

    @Autowired
    AiUsageRepository usageRepository;

    @Test
    @DisplayName("기록이 없으면 이번 주도 이번 달도 비어 있다")
    void emptyInitially() throws Exception {
        mvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.week").isEmpty())
                .andExpect(jsonPath("$.month").isEmpty());
    }

    @Test
    @DisplayName("제공자별로 호출 수와 토큰을 모으고 많이 부른 제공자가 앞에 온다")
    void sumsByProvider() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        insert(defaultUser(), GEMINI, 10, 1, now);
        insert(defaultUser(), OPENAI, 100, 20, now);
        insert(defaultUser(), OPENAI, 50, 5, now);

        mvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.week.length()").value(2))
                .andExpect(jsonPath("$.week[0].baseUrl").value(OPENAI))
                .andExpect(jsonPath("$.week[0].calls").value(2))
                .andExpect(jsonPath("$.week[0].inputTokens").value(150))
                .andExpect(jsonPath("$.week[0].outputTokens").value(25))
                .andExpect(jsonPath("$.week[1].baseUrl").value(GEMINI))
                .andExpect(jsonPath("$.month[0].calls").value(2));
    }

    @Test
    @DisplayName("이번 달이 시작하기 전의 기록은 이번 달에 넣지 않는다")
    void excludesBeforeMonthStart() throws Exception {
        LocalDateTime beforeMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay().minusHours(1);
        insert(defaultUser(), OPENAI, 100, 20, beforeMonth);

        mvc.perform(get(PATH))
                .andExpect(jsonPath("$.month").isEmpty());
    }

    @Test
    @DisplayName("이번 주 월요일 전의 기록은 이번 주에 넣지 않는다")
    void excludesBeforeWeekStart() throws Exception {
        LocalDateTime beforeWeek = LocalDate.now().with(DayOfWeek.MONDAY).atStartOfDay().minusHours(1);
        insert(defaultUser(), OPENAI, 100, 20, beforeWeek);

        mvc.perform(get(PATH))
                .andExpect(jsonPath("$.week").isEmpty());
    }

    @Test
    @DisplayName("기록할 때 석 달이 지난 기록을 지운다")
    void recordPrunesOlderThanThreeMonths() {
        LocalDateTime now = LocalDateTime.now();
        insert(defaultUser(), OPENAI, 1, 1, now.minusMonths(3).minusDays(1));
        insert(defaultUser(), OPENAI, 1, 1, now.minusMonths(3).plusDays(1));

        usageService.record(OPENAI, "gpt-5.6-luna", new AiUsage(7, 3));

        // 지우기는 flush 때 나간다. jdbc 는 flush 를 일으키지 않아 리포지토리로 센다
        // 석 달 안쪽 하나와 방금 적은 하나만 남는다
        assertThat(usageRepository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("같은 제공자를 다르게 적어도 한 줄로 모인다")
    void recordNormalizesProvider() throws Exception {
        usageService.record(OPENAI + "/", "gpt-5.6-luna", new AiUsage(7, 3));
        usageService.record("HTTPS://API.OPENAI.COM/v1", "gpt-5.6-luna", new AiUsage(1, 1));

        mvc.perform(get(PATH))
                .andExpect(jsonPath("$.week.length()").value(1))
                .andExpect(jsonPath("$.week[0].baseUrl").value(OPENAI))
                .andExpect(jsonPath("$.week[0].calls").value(2));
    }

    @Test
    @DisplayName("남의 사용량은 섞이지 않는다")
    void excludesOtherUsers() throws Exception {
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"other\",\"displayName\":\"other\"}"))
                .andExpect(status().isCreated());
        Long other = jdbc.queryForObject("select id from app_user where username = 'other'", Long.class);
        insert(other, OPENAI, 100, 20, LocalDateTime.now());

        mvc.perform(get(PATH))
                .andExpect(jsonPath("$.week").isEmpty());
    }

    // --- 헬퍼 -------------------------------------------------------------

    private Long defaultUser() {
        return jdbc.queryForObject("select id from app_user where username = 'default'", Long.class);
    }

    private void insert(Long userId, String baseUrl, int in, int out, LocalDateTime at) {
        jdbc.update("insert into ai_usage (user_id, base_url, model, input_tokens, output_tokens, created_at) "
                + "values (?, ?, 'm', ?, ?, ?)", userId, baseUrl, in, out, at);
    }
}
