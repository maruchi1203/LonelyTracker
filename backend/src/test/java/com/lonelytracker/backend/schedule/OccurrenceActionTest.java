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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 회차 단위 동작.
 * <p>
 * 핵심은 <b>onDate 가 회차의 정체성</b>이라는 것이다. 미뤄도 onDate 는 안 바뀌고
 * startAt 만 옮겨간다. 그래서 한 행이 "계획했던 날" 과 "실제로 간 날" 을 동시에 갖는다.
 */
@AutoConfigureMockMvc
@Transactional
class OccurrenceActionTest extends IntegrationTest {

    private static final String SERIES = "/api/series";
    private static final String SCHEDULES = "/api/schedules";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @Test
    @DisplayName("회차를 완료로 바꾸면 그 회차만 DONE 이 된다")
    void completeOneOccurrence() throws Exception {
        long seriesId = dailySeries("독서", "2026-09-01T21:00:00");

        mvc.perform(patch(SERIES + "/" + seriesId + "/occurrences/2026-09-02/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.occurrenceDate").value("2026-09-02"));

        List<JsonNode> found = occurrencesOf(seriesId, "2026-09-01T00:00:00", "2026-09-03T23:59:59");

        assertThat(fieldAt(found, "2026-09-01", "status")).isEqualTo("PLANNED");
        assertThat(fieldAt(found, "2026-09-02", "status")).isEqualTo("DONE");
        assertThat(fieldAt(found, "2026-09-03", "status")).isEqualTo("PLANNED");
    }

    @Test
    @DisplayName("연기하면 onDate 는 그대로고 startAt 만 옮겨간다")
    void postponeKeepsOnDate() throws Exception {
        long seriesId = dailySeries("운동", "2026-09-01T07:00:00");

        mvc.perform(patch(SERIES + "/" + seriesId + "/occurrences/2026-09-02/postpone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"to\":\"2026-09-04T19:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.occurrenceDate").value("2026-09-02"))
                .andExpect(jsonPath("$.startAt").value("2026-09-04T19:00:00"))
                .andExpect(jsonPath("$.postponeCount").value(1));
    }

    @Test
    @DisplayName("연기해도 소요시간이 유지된다")
    void postponeKeepsDuration() throws Exception {
        long seriesId = createSeries("""
                { "title": "회의", "startAt": "2026-09-01T10:00:00",
                  "endAt": "2026-09-01T11:30:00",
                  "recurrence": { "freq": "DAILY" } }""");

        mvc.perform(patch(SERIES + "/" + seriesId + "/occurrences/2026-09-02/postpone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"to\":\"2026-09-05T14:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startAt").value("2026-09-05T14:00:00"))
                .andExpect(jsonPath("$.endAt").value("2026-09-05T15:30:00"));
    }

    @Test
    @DisplayName("두 번 미루면 postponeCount 가 2 이고 회차는 여전히 하나다")
    void postponeTwiceKeepsSingleOccurrence() throws Exception {
        long seriesId = dailySeries("보고서", "2026-09-01T09:00:00");
        String occurrence = SERIES + "/" + seriesId + "/occurrences/2026-09-01/postpone";

        mvc.perform(patch(occurrence).contentType(MediaType.APPLICATION_JSON)
                .content("{\"to\":\"2026-09-02T09:00:00\"}")).andExpect(status().isOk());
        mvc.perform(patch(occurrence).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"to\":\"2026-09-03T09:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postponeCount").value(2));

        List<JsonNode> found = occurrencesOf(seriesId, "2026-09-01T00:00:00", "2026-09-05T23:59:59");
        long withOnDate = found.stream()
                .filter(n -> "2026-09-01".equals(n.get("occurrenceDate").asString()))
                .count();

        assertThat(withOnDate).as("미뤄도 회차가 늘어나면 안 된다").isEqualTo(1);
    }

    @Test
    @DisplayName("월말에서 다음 달로 미루면 두 달 조회 모두에 나온다")
    void postponedAcrossMonthsAppearsInBoth() throws Exception {
        long seriesId = dailySeries("마감", "2026-08-30T09:00:00");

        mvc.perform(patch(SERIES + "/" + seriesId + "/occurrences/2026-08-31/postpone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"to\":\"2026-09-02T09:00:00\"}"))
                .andExpect(status().isOk());

        List<JsonNode> august = occurrencesOf(seriesId, "2026-08-01T00:00:00", "2026-08-31T23:59:59");
        List<JsonNode> september = occurrencesOf(seriesId, "2026-09-01T00:00:00", "2026-09-30T23:59:59");

        // 8월: onDate 로 잡힌다. "9/2로 미룸" 을 그릴 수 있어야 한다
        assertThat(august).anyMatch(n -> "2026-08-31".equals(n.get("occurrenceDate").asString()));
        // 9월: startAt 으로 잡힌다
        assertThat(september).anyMatch(n -> "2026-08-31".equals(n.get("occurrenceDate").asString()));
    }

    @Test
    @DisplayName("건너뛰기는 postponeCount 를 건드리지 않는다")
    void skipDoesNotTouchPostponeCount() throws Exception {
        long seriesId = dailySeries("운동", "2026-09-01T07:00:00");

        mvc.perform(patch(SERIES + "/" + seriesId + "/occurrences/2026-09-02/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SKIPPED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SKIPPED"))
                .andExpect(jsonPath("$.postponeCount").value(0));
    }

    @Test
    @DisplayName("미뤘다가 완료하면 상태와 미룸 횟수가 함께 남는다")
    void postponeThenCompleteKeepsBoth() throws Exception {
        long seriesId = dailySeries("운동", "2026-09-01T07:00:00");

        mvc.perform(patch(SERIES + "/" + seriesId + "/occurrences/2026-09-02/postpone")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"to\":\"2026-09-03T07:00:00\"}")).andExpect(status().isOk());

        // 결국 해냈다. 수행률에는 DONE 으로 잡히고 "밀렸다" 는 사실은 따로 남는다
        mvc.perform(patch(SERIES + "/" + seriesId + "/occurrences/2026-09-02/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.postponeCount").value(1))
                .andExpect(jsonPath("$.startAt").value("2026-09-03T07:00:00"));
    }

    @Test
    @DisplayName("단일 일정도 같은 방식으로 미뤄진다")
    void postponeSingleSchedule() throws Exception {
        JsonNode created = json(mvc.perform(post(SCHEDULES).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "보고서", "startAt": "2026-09-01T09:00:00",
                                  "endAt": "2026-09-01T10:30:00" }"""))
                .andExpect(status().isCreated()));

        mvc.perform(patch(SCHEDULES + "/" + created.get("id").asLong() + "/postpone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"to\":\"2026-09-03T14:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startAt").value("2026-09-03T14:00:00"))
                // 소요시간 유지
                .andExpect(jsonPath("$.endAt").value("2026-09-03T15:30:00"))
                .andExpect(jsonPath("$.postponeCount").value(1));
    }

    @Test
    @DisplayName("규칙이 만들지 않는 날짜의 회차는 404 다")
    void unknownOccurrenceIsNotFound() throws Exception {
        long seriesId = createSeries("""
                { "title": "운동", "startAt": "2026-09-07T07:00:00",
                  "recurrence": { "freq": "WEEKLY", "byWeekday": ["MONDAY"] } }""");

        // 2026-09-08 은 화요일이라 이 규칙에 없는 날이다
        mvc.perform(patch(SERIES + "/" + seriesId + "/occurrences/2026-09-08/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("없는 시리즈는 404 다")
    void unknownSeriesIsNotFound() throws Exception {
        mvc.perform(patch(SERIES + "/99999999/occurrences/2026-09-01/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isNotFound());
    }

    // --- 헬퍼 -------------------------------------------------------------

    private long dailySeries(String title, String startAt) throws Exception {
        return createSeries("""
                { "title": "%s", "startAt": "%s",
                  "recurrence": { "freq": "DAILY" } }""".formatted(title, startAt));
    }

    private long createSeries(String body) throws Exception {
        JsonNode created = json(mvc.perform(post(SERIES)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()));
        return created.get("seriesId").asLong();
    }

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

    private String fieldAt(List<JsonNode> nodes, String occurrenceDate, String field) {
        return nodes.stream()
                .filter(n -> occurrenceDate.equals(n.get("occurrenceDate").asString()))
                .map(n -> n.get(field).asString())
                .findFirst()
                .orElseThrow(() -> new AssertionError("회차가 없다: " + occurrenceDate));
    }

    private JsonNode json(ResultActions actions) throws Exception {
        return mapper.readTree(actions.andReturn().getResponse().getContentAsString());
    }
}
