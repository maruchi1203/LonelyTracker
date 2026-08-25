package com.lonelytracker.backend.schedule;

import com.lonelytracker.backend.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 반복 일정 생성과 조회 전개.
 * <p>
 * 회차는 행으로 존재하지 않는다. 조회할 때마다 규칙에서 펼쳐진다.
 * 그래서 "몇 행 생겼나" 가 아니라 "조회하면 어떤 날짜가 나오나" 가 검증 대상이다.
 */
@AutoConfigureMockMvc
@Transactional
class ScheduleSeriesApiTest extends IntegrationTest {

    private static final String SERIES = "/api/series";
    private static final String SCHEDULES = "/api/schedules";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @Test
    @DisplayName("반복 생성은 시리즈 1행만 만들고 첫 회차를 돌려준다")
    void createsSeriesOnly() throws Exception {
        mvc.perform(post(SERIES).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "운동",
                                  "startAt": "2026-09-07T07:00:00",
                                  "endAt": "2026-09-07T08:00:00",
                                  "category": "육체",
                                  "recurrence": {
                                    "freq": "WEEKLY",
                                    "byWeekday": ["MONDAY", "WEDNESDAY", "FRIDAY"]
                                  }
                                }"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.seriesId").isNumber())
                .andExpect(jsonPath("$.firstOccurrence.title").value("운동"))
                // 회차는 행이 아니므로 id 가 없다
                .andExpect(jsonPath("$.firstOccurrence.id").doesNotExist())
                .andExpect(jsonPath("$.firstOccurrence.occurrenceDate").value("2026-09-07"))
                .andExpect(jsonPath("$.firstOccurrence.status").value("PLANNED"));
    }

    @Test
    @DisplayName("조회하면 규칙대로 그 요일에만 회차가 나온다")
    void expandsOnRequestedWeekdays() throws Exception {
        long seriesId = createSeries("""
                {
                  "title": "운동",
                  "startAt": "2026-09-07T07:00:00",
                  "recurrence": { "freq": "WEEKLY", "byWeekday": ["MONDAY", "WEDNESDAY", "FRIDAY"] }
                }""");

        List<JsonNode> week = occurrencesOf(seriesId, "2026-09-07T00:00:00", "2026-09-13T23:59:59");

        assertThat(datesOf(week)).containsExactly("2026-09-07", "2026-09-09", "2026-09-11");
    }

    @Test
    @DisplayName("모든 회차가 시리즈의 시각과 소요시간을 물려받는다")
    void inheritsTemplateTime() throws Exception {
        long seriesId = createSeries("""
                {
                  "title": "아침 운동",
                  "startAt": "2026-09-07T07:00:00",
                  "endAt": "2026-09-07T08:30:00",
                  "recurrence": { "freq": "WEEKLY", "byWeekday": ["MONDAY"] }
                }""");

        List<JsonNode> found = occurrencesOf(seriesId, "2026-09-01T00:00:00", "2026-09-30T23:59:59");

        assertThat(found).isNotEmpty();
        found.forEach(node -> {
            assertThat(node.get("startAt").asString()).endsWith("T07:00:00");
            assertThat(node.get("endAt").asString()).endsWith("T08:30:00");
        });
    }

    @Test
    @DisplayName("무기한 시리즈도 조회 범위만큼만 나온다")
    void openEndedSeriesIsBoundedByQuery() throws Exception {
        long seriesId = createSeries("""
                {
                  "title": "매일 독서",
                  "startAt": "2026-09-01T21:00:00",
                  "recurrence": { "freq": "DAILY" }
                }""");

        List<JsonNode> week = occurrencesOf(seriesId, "2026-09-01T00:00:00", "2026-09-07T23:59:59");

        assertThat(week).hasSize(7);
    }

    @Test
    @DisplayName("종료일 이후는 나오지 않는다")
    void respectsEndDate() throws Exception {
        long seriesId = createSeries("""
                {
                  "title": "짧은 반복",
                  "startAt": "2026-09-01T09:00:00",
                  "recurrence": { "freq": "DAILY", "endsOn": "2026-09-05" }
                }""");

        List<JsonNode> found = occurrencesOf(seriesId, "2026-09-01T00:00:00", "2026-09-30T23:59:59");

        assertThat(datesOf(found)).containsExactly(
                "2026-09-01", "2026-09-02", "2026-09-03", "2026-09-04", "2026-09-05");
    }

    @Test
    @DisplayName("시작일 이전은 나오지 않는다")
    void respectsStartDate() throws Exception {
        long seriesId = createSeries("""
                {
                  "title": "나중 시작",
                  "startAt": "2026-09-15T09:00:00",
                  "recurrence": { "freq": "DAILY", "endsOn": "2026-09-17" }
                }""");

        List<JsonNode> found = occurrencesOf(seriesId, "2026-09-01T00:00:00", "2026-09-30T23:59:59");

        assertThat(datesOf(found)).containsExactly("2026-09-15", "2026-09-16", "2026-09-17");
    }

    @Test
    @DisplayName("매주인데 요일을 안 고르면 400을 반환한다")
    void rejectsWeeklyWithoutWeekday() throws Exception {
        mvc.perform(post(SERIES).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "요일 없음",
                                  "startAt": "2026-09-07T09:00:00",
                                  "recurrence": { "freq": "WEEKLY", "byWeekday": [] }
                                }"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("title이 없으면 400을 반환한다")
    void rejectsWithoutTitle() throws Exception {
        mvc.perform(post(SERIES).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "startAt": "2026-09-07T09:00:00",
                                  "recurrence": { "freq": "DAILY" }
                                }"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("단일 일정은 seriesId 없이 그대로 나온다")
    void singleScheduleIsUnaffected() throws Exception {
        mvc.perform(post(SCHEDULES).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "혼자", "startAt": "2026-09-08T09:00:00" }"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.seriesId").doesNotExist())
                .andExpect(jsonPath("$.postponeCount").value(0));
    }

    // --- 헬퍼 -------------------------------------------------------------

    private long createSeries(String body) throws Exception {
        JsonNode created = json(mvc.perform(post(SERIES)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()));
        return created.get("seriesId").asLong();
    }

    /** 기간 조회 결과에서 그 시리즈의 회차만 골라낸다. */
    private List<JsonNode> occurrencesOf(long seriesId, String from, String to) throws Exception {
        JsonNode all = json(mvc.perform(get(SCHEDULES).param("from", from).param("to", to))
                .andExpect(status().isOk()));
        List<JsonNode> mine = new ArrayList<>();
        for (JsonNode node : all) {
            JsonNode id = node.get("seriesId");
            if (id != null && !id.isNull() && id.asLong() == seriesId) {
                mine.add(node);
            }
        }
        return mine;
    }

    private List<String> datesOf(List<JsonNode> nodes) {
        return nodes.stream().map(n -> n.get("occurrenceDate").asString()).toList();
    }

    private JsonNode json(ResultActions actions) throws Exception {
        return mapper.readTree(actions.andReturn().getResponse().getContentAsString());
    }
}
