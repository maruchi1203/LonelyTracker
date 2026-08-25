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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 시리즈 수정·삭제.
 * <p>
 * 전개가 조회 시점이므로 규칙을 바꾸면 <b>즉시</b> 조회 결과가 달라진다.
 * 회차를 미리 만드는 방식이었다면 "미래 회차를 지우고 재생성" 이 필요했을 자리다.
 * <p>
 * 그만두기는 {@code LocalDate.now()} 를 기준으로 동작하므로, 관련 테스트는
 * 고정 날짜 대신 <b>오늘로부터의 상대 날짜</b>로 짠다. 고정 날짜로 짜면
 * 시간이 흐르면서 조용히 깨진다.
 */
@AutoConfigureMockMvc
@Transactional
class SeriesEditTest extends IntegrationTest {

    private static final String SERIES = "/api/series";
    private static final String SCHEDULES = "/api/schedules";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @Test
    @DisplayName("요일을 바꾸면 조회 결과가 즉시 달라진다")
    void changingWeekdaysTakesEffectImmediately() throws Exception {
        long seriesId = createSeries("""
                { "title": "운동", "startAt": "2026-09-07T07:00:00",
                  "recurrence": { "freq": "WEEKLY", "byWeekday": ["MONDAY"] } }""");

        mvc.perform(put(SERIES + "/" + seriesId).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "운동", "startTime": "07:00:00",
                                  "recurrence": { "freq": "WEEKLY", "byWeekday": ["TUESDAY"] } }"""))
                .andExpect(status().isOk());

        List<JsonNode> week = occurrencesOf(seriesId, "2026-09-07T00:00:00", "2026-09-13T23:59:59");

        // 2026-09-08 이 화요일이다
        assertThat(datesOf(week)).containsExactly("2026-09-08");
    }

    @Test
    @DisplayName("앞으로 전부 수정은 시리즈만 바꾸고 완료 기록은 그대로 둔다")
    void templateUpdateKeepsOverrides() throws Exception {
        long seriesId = createSeries("""
                { "title": "원래 제목", "startAt": "2026-09-01T09:00:00",
                  "recurrence": { "freq": "DAILY" } }""");

        markDone(seriesId, "2026-09-02");

        mvc.perform(put(SERIES + "/" + seriesId).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "바뀐 제목", "startTime": "09:00:00",
                                  "recurrence": { "freq": "DAILY" } }"""))
                .andExpect(status().isOk());

        List<JsonNode> found = occurrencesOf(seriesId, "2026-09-01T00:00:00", "2026-09-03T23:59:59");

        assertThat(found).allMatch(n -> "바뀐 제목".equals(n.get("title").asString()));
        assertThat(fieldAt(found, "2026-09-02", "status"))
                .as("완료 기록은 템플릿 수정에 영향받지 않는다").isEqualTo("DONE");
    }

    @Test
    @DisplayName("이 회차만 수정하면 다른 회차는 그대로다")
    void updateOneOccurrence() throws Exception {
        long seriesId = createSeries("""
                { "title": "운동", "startAt": "2026-09-01T07:00:00",
                  "recurrence": { "freq": "DAILY" } }""");

        mvc.perform(put(SERIES + "/" + seriesId + "/occurrences/2026-09-02")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "가벼운 운동", "startAt": "2026-09-02T20:00:00" }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("가벼운 운동"));

        List<JsonNode> found = occurrencesOf(seriesId, "2026-09-01T00:00:00", "2026-09-03T23:59:59");

        assertThat(fieldAt(found, "2026-09-01", "title")).isEqualTo("운동");
        assertThat(fieldAt(found, "2026-09-02", "title")).isEqualTo("가벼운 운동");
        assertThat(fieldAt(found, "2026-09-03", "title")).isEqualTo("운동");
    }

    @Test
    @DisplayName("이 회차만 수정해도 시리즈 템플릿은 그대로다")
    void occurrenceUpdateDoesNotTouchTemplate() throws Exception {
        long seriesId = createSeries("""
                { "title": "운동", "startAt": "2026-09-01T07:00:00",
                  "category": "육체", "recurrence": { "freq": "DAILY" } }""");

        mvc.perform(put(SERIES + "/" + seriesId + "/occurrences/2026-09-02")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "title": "가벼운 운동", "startAt": "2026-09-02T20:00:00",
                          "category": "취미" }""")).andExpect(status().isOk());

        List<JsonNode> found = occurrencesOf(seriesId, "2026-09-03T00:00:00", "2026-09-03T23:59:59");

        assertThat(fieldAt(found, "2026-09-03", "category")).isEqualTo("육체");
    }

    @Test
    @DisplayName("그만두면 미래 회차가 사라진다")
    void stopRemovesFutureOccurrences() throws Exception {
        LocalDate start = LocalDate.now().minusDays(10);
        long seriesId = dailySeriesFrom(start, "지금 그만둘 습관");

        mvc.perform(delete(SERIES + "/" + seriesId).param("scope", "FUTURE"))
                .andExpect(status().isNoContent());

        List<JsonNode> future = occurrencesOf(seriesId,
                LocalDate.now().plusDays(1) + "T00:00:00",
                LocalDate.now().plusDays(10) + "T23:59:59");

        assertThat(future).isEmpty();
    }

    @Test
    @DisplayName("그만둬도 지난 기록은 남는다")
    void stopKeepsPastRecords() throws Exception {
        LocalDate start = LocalDate.now().minusDays(10);
        LocalDate doneOn = LocalDate.now().minusDays(5);
        long seriesId = dailySeriesFrom(start, "지난 습관");

        markDone(seriesId, doneOn.toString());

        mvc.perform(delete(SERIES + "/" + seriesId).param("scope", "FUTURE"))
                .andExpect(status().isNoContent());

        List<JsonNode> past = occurrencesOf(seriesId,
                start + "T00:00:00", LocalDate.now() + "T23:59:59");

        assertThat(past).isNotEmpty();
        assertThat(fieldAt(past, doneOn.toString(), "status")).isEqualTo("DONE");
    }

    @Test
    @DisplayName("전체 삭제는 지난 기록까지 지운다")
    void deleteAllCascades() throws Exception {
        LocalDate start = LocalDate.now().minusDays(10);
        LocalDate doneOn = LocalDate.now().minusDays(5);
        long seriesId = dailySeriesFrom(start, "통째로 삭제");

        markDone(seriesId, doneOn.toString());

        mvc.perform(delete(SERIES + "/" + seriesId).param("scope", "ALL"))
                .andExpect(status().isNoContent());

        assertThat(occurrencesOf(seriesId, start + "T00:00:00", LocalDate.now() + "T23:59:59"))
                .isEmpty();
    }

    @Test
    @DisplayName("scope 를 빠뜨리면 그만두기로 동작해 지난 기록이 살아남는다")
    void deleteDefaultsToStop() throws Exception {
        LocalDate start = LocalDate.now().minusDays(10);
        LocalDate doneOn = LocalDate.now().minusDays(5);
        long seriesId = dailySeriesFrom(start, "기본값 확인");

        markDone(seriesId, doneOn.toString());

        mvc.perform(delete(SERIES + "/" + seriesId)).andExpect(status().isNoContent());

        List<JsonNode> past = occurrencesOf(seriesId,
                start + "T00:00:00", LocalDate.now() + "T23:59:59");

        assertThat(fieldAt(past, doneOn.toString(), "status"))
                .as("기본값이 ALL 이면 과거 기록이 사라진다").isEqualTo("DONE");
    }

    @Test
    @DisplayName("없는 시리즈를 수정하면 404 다")
    void updateMissingSeries() throws Exception {
        mvc.perform(put(SERIES + "/99999999").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "없음", "startTime": "09:00:00",
                                  "recurrence": { "freq": "DAILY" } }"""))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("없는 시리즈를 삭제하면 404 다")
    void deleteMissingSeries() throws Exception {
        mvc.perform(delete(SERIES + "/99999999")).andExpect(status().isNotFound());
    }

    // --- 헬퍼 -------------------------------------------------------------

    private long dailySeriesFrom(LocalDate start, String title) throws Exception {
        return createSeries("""
                { "title": "%s", "startAt": "%sT09:00:00",
                  "recurrence": { "freq": "DAILY" } }""".formatted(title, start));
    }

    private long createSeries(String body) throws Exception {
        JsonNode created = json(mvc.perform(post(SERIES)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()));
        return created.get("seriesId").asLong();
    }

    private void markDone(long seriesId, String onDate) throws Exception {
        mvc.perform(patch(SERIES + "/" + seriesId + "/occurrences/" + onDate + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk());
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

    private List<String> datesOf(List<JsonNode> nodes) {
        return nodes.stream().map(n -> n.get("occurrenceDate").asString()).toList();
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
