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
 * 회차 단위 동작 — 완료 · 건너뛰기 · 연기 · 수정 · 삭제.
 * <p>
 * 핵심은 <b>onDate 가 회차의 정체성</b>이라는 것이다. 미뤄도 onDate 는 안 바뀌고
 * startAt 만 옮겨간다. 그래서 한 행이 "계획했던 날" 과 "실제로 간 날" 을 동시에 갖는다.
 * <p>
 * 그만두기는 {@code LocalDate.now()} 기준이므로 관련 테스트는 고정 날짜 대신
 * <b>오늘로부터의 상대 날짜</b>로 짠다. 고정 날짜로 짜면 시간이 흐르며 조용히 깨진다.
 */
@AutoConfigureMockMvc
@Transactional
class InstanceApiTest extends IntegrationTest {

    private static final String BASE = "/api/schedules";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    // --- 상태 -------------------------------------------------------------

    @Test
    @DisplayName("회차를 완료로 바꾸면 그 회차만 DONE이 된다")
    void completeOneInstance() throws Exception {
        long id = daily("독서", "2026-09-01T21:00:00");

        mvc.perform(patch(BASE + "/" + id + "/instances/2026-09-02/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.instanceDate").value("2026-09-02"));

        List<JsonNode> found = instancesOf(id, "2026-09-01T00:00:00", "2026-09-03T23:59:59");

        assertThat(fieldAt(found, "2026-09-01", "status")).isEqualTo("PLANNED");
        assertThat(fieldAt(found, "2026-09-02", "status")).isEqualTo("DONE");
        assertThat(fieldAt(found, "2026-09-03", "status")).isEqualTo("PLANNED");
    }

    @Test
    @DisplayName("규칙이 만들지 않는 날짜의 회차는 404다")
    void unknownInstanceIsNotFound() throws Exception {
        long id = createSchedule("""
                { "title": "운동", "startAt": "2026-09-07T07:00:00",
                  "recurrence": { "freq": "WEEKLY", "byWeekday": ["MONDAY"] } }""");

        // 2026-09-08은 화요일이라 이 규칙에 없는 날이다
        mvc.perform(patch(BASE + "/" + id + "/instances/2026-09-08/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isNotFound());
    }

    // --- 연기 -------------------------------------------------------------

    @Test
    @DisplayName("연기하면 onDate는 그대로고 startAt만 옮겨간다")
    void postponeKeepsOnDate() throws Exception {
        long id = daily("운동", "2026-09-01T07:00:00");

        mvc.perform(patch(BASE + "/" + id + "/instances/2026-09-02/postpone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"to\":\"2026-09-04T19:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instanceDate").value("2026-09-02"))
                .andExpect(jsonPath("$.startAt").value("2026-09-04T19:00:00"))
                .andExpect(jsonPath("$.postponeCount").value(1));
    }

    @Test
    @DisplayName("두 번 미루면 postponeCount가 2이고 회차는 여전히 하나다")
    void postponeTwiceKeepsSingleInstance() throws Exception {
        long id = daily("보고서", "2026-09-01T09:00:00");
        String path = BASE + "/" + id + "/instances/2026-09-01/postpone";

        mvc.perform(patch(path).contentType(MediaType.APPLICATION_JSON)
                .content("{\"to\":\"2026-09-02T09:00:00\"}")).andExpect(status().isOk());
        mvc.perform(patch(path).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"to\":\"2026-09-03T09:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.postponeCount").value(2));

        List<JsonNode> found = instancesOf(id, "2026-09-01T00:00:00", "2026-09-05T23:59:59");
        long sameOnDate = found.stream()
                .filter(n -> "2026-09-01".equals(n.get("instanceDate").asString()))
                .count();

        assertThat(sameOnDate).as("미뤄도 회차가 늘어나면 안 된다").isEqualTo(1);
    }

    @Test
    @DisplayName("월말에서 다음 달로 미루면 두 달 조회 모두에 나온다")
    void postponedAcrossMonthsAppearsInBoth() throws Exception {
        long id = daily("마감", "2026-08-30T09:00:00");

        mvc.perform(patch(BASE + "/" + id + "/instances/2026-08-31/postpone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"to\":\"2026-09-02T09:00:00\"}"))
                .andExpect(status().isOk());

        // 8월: onDate로 잡힌다 ("9/2로 미룸"을 그릴 수 있어야 한다)
        assertThat(instancesOf(id, "2026-08-01T00:00:00", "2026-08-31T23:59:59"))
                .anyMatch(n -> "2026-08-31".equals(n.get("instanceDate").asString()));
        // 9월: startAt으로 잡힌다
        assertThat(instancesOf(id, "2026-09-01T00:00:00", "2026-09-30T23:59:59"))
                .anyMatch(n -> "2026-08-31".equals(n.get("instanceDate").asString()));
    }

    @Test
    @DisplayName("건너뛰기는 postponeCount를 건드리지 않는다")
    void skipDoesNotTouchPostponeCount() throws Exception {
        long id = daily("운동", "2026-09-01T07:00:00");

        mvc.perform(patch(BASE + "/" + id + "/instances/2026-09-02/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SKIPPED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SKIPPED"))
                .andExpect(jsonPath("$.postponeCount").value(0));
    }

    @Test
    @DisplayName("미뤘다가 완료하면 상태와 미룸 횟수가 함께 남는다")
    void postponeThenCompleteKeepsBoth() throws Exception {
        long id = daily("운동", "2026-09-01T07:00:00");

        mvc.perform(patch(BASE + "/" + id + "/instances/2026-09-02/postpone")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"to\":\"2026-09-03T07:00:00\"}")).andExpect(status().isOk());

        // 결국 해냈다. 수행률에는 DONE으로 잡히고 "밀렸다"는 사실은 따로 남는다
        mvc.perform(patch(BASE + "/" + id + "/instances/2026-09-02/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.postponeCount").value(1))
                .andExpect(jsonPath("$.startAt").value("2026-09-03T07:00:00"));
    }

    @Test
    @DisplayName("연기가 개별 수정한 소요시간을 되돌리지 않는다")
    void postponeKeepsIndividuallyEditedDuration() throws Exception {
        // 일정 기본은 1시간
        long id = createSchedule("""
                { "title": "회의", "startAt": "2026-09-01T10:00:00",
                  "endAt": "2026-09-01T11:00:00",
                  "recurrence": { "freq": "DAILY" } }""");

        // 9/2 회차만 2시간으로 늘린다
        mvc.perform(put(BASE + "/" + id + "/instances/2026-09-02")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "startAt": "2026-09-02T10:00:00", "endAt": "2026-09-02T12:00:00" }"""))
                .andExpect(status().isOk());

        // 미뤄도 2시간이어야 한다. 기본값으로 되돌리면 사용자가 지정한 값이 사라진다
        mvc.perform(patch(BASE + "/" + id + "/instances/2026-09-02/postpone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"to\":\"2026-09-05T14:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startAt").value("2026-09-05T14:00:00"))
                .andExpect(jsonPath("$.endAt").value("2026-09-05T16:00:00"));
    }

    @Test
    @DisplayName("단일 일정도 같은 방식으로 미뤄진다")
    void postponeSingleSchedule() throws Exception {
        long id = createSchedule("""
                { "title": "보고서", "startAt": "2026-09-01T09:00:00",
                  "endAt": "2026-09-01T10:30:00" }""");

        mvc.perform(patch(BASE + "/" + id + "/instances/2026-09-01/postpone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"to\":\"2026-09-03T14:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.instanceDate").value("2026-09-01"))
                .andExpect(jsonPath("$.startAt").value("2026-09-03T14:00:00"))
                .andExpect(jsonPath("$.endAt").value("2026-09-03T15:30:00"))
                .andExpect(jsonPath("$.postponeCount").value(1));
    }

    // --- 수정 -------------------------------------------------------------

    @Test
    @DisplayName("요일을 바꾸면 조회 결과가 즉시 달라진다")
    void changingWeekdaysTakesEffectImmediately() throws Exception {
        long id = createSchedule("""
                { "title": "운동", "startAt": "2026-09-07T07:00:00",
                  "recurrence": { "freq": "WEEKLY", "byWeekday": ["MONDAY"] } }""");

        mvc.perform(put(BASE + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "운동", "startAt": "2026-09-07T07:00:00",
                                  "recurrence": { "freq": "WEEKLY", "byWeekday": ["TUESDAY"] } }"""))
                .andExpect(status().isOk());

        // 2026-09-08이 화요일이다
        assertThat(datesOf(instancesOf(id, "2026-09-07T00:00:00", "2026-09-13T23:59:59")))
                .containsExactly("2026-09-08");
    }

    @Test
    @DisplayName("앞으로 전부 수정해도 완료 기록은 그대로다")
    void templateUpdateKeepsProgress() throws Exception {
        long id = daily("원래 제목", "2026-09-01T09:00:00");
        markDone(id, "2026-09-02");

        mvc.perform(put(BASE + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "바뀐 제목", "startAt": "2026-09-01T09:00:00",
                                  "recurrence": { "freq": "DAILY" } }"""))
                .andExpect(status().isOk());

        List<JsonNode> found = instancesOf(id, "2026-09-01T00:00:00", "2026-09-03T23:59:59");

        assertThat(found).allMatch(n -> "바뀐 제목".equals(n.get("title").asString()));
        assertThat(fieldAt(found, "2026-09-02", "status")).isEqualTo("DONE");
    }

    @Test
    @DisplayName("이 회차만 수정하면 다른 회차는 그대로다")
    void updateOneInstance() throws Exception {
        long id = daily("운동", "2026-09-01T07:00:00");

        mvc.perform(put(BASE + "/" + id + "/instances/2026-09-02")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "가벼운 운동", "startAt": "2026-09-02T20:00:00" }"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("가벼운 운동"));

        List<JsonNode> found = instancesOf(id, "2026-09-01T00:00:00", "2026-09-03T23:59:59");

        assertThat(fieldAt(found, "2026-09-01", "title")).isEqualTo("운동");
        assertThat(fieldAt(found, "2026-09-02", "title")).isEqualTo("가벼운 운동");
        assertThat(fieldAt(found, "2026-09-03", "title")).isEqualTo("운동");
    }

    @Test
    @DisplayName("이 회차만 수정을 빈 값으로 보내면 일정 값으로 되돌아간다")
    void instanceOverrideCanBeReverted() throws Exception {
        long id = daily("운동", "2026-09-01T07:00:00");

        mvc.perform(put(BASE + "/" + id + "/instances/2026-09-02")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "title": "가벼운 운동", "startAt": "2026-09-02T20:00:00" }"""))
                .andExpect(status().isOk());

        // 모든 필드가 선택이라 빈 본문으로 되돌릴 수 있다
        mvc.perform(put(BASE + "/" + id + "/instances/2026-09-02")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("운동"))
                .andExpect(jsonPath("$.startAt").value("2026-09-02T07:00:00"));
    }

    // --- 삭제 -------------------------------------------------------------

    @Test
    @DisplayName("그만두면 미래 회차가 사라진다")
    void stopRemovesFutureInstances() throws Exception {
        LocalDate start = LocalDate.now().minusDays(10);
        long id = dailyFrom(start, "그만둘 습관");

        mvc.perform(delete(BASE + "/" + id).param("scope", "FUTURE"))
                .andExpect(status().isNoContent());

        assertThat(instancesOf(id,
                LocalDate.now().plusDays(1) + "T00:00:00",
                LocalDate.now().plusDays(10) + "T23:59:59")).isEmpty();
    }

    @Test
    @DisplayName("그만두면 미뤄둔 미래 회차도 함께 사라진다")
    void stopAlsoRemovesPostponedFutureInstances() throws Exception {
        LocalDate start = LocalDate.now().minusDays(10);
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        long id = dailyFrom(start, "미뤄둔 습관");

        // 내일 회차를 다음 주로 미룬다
        mvc.perform(patch(BASE + "/" + id + "/instances/" + tomorrow + "/postpone")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"to\":\"" + LocalDate.now().plusDays(7) + "T09:00:00\"}"))
                .andExpect(status().isOk());

        mvc.perform(delete(BASE + "/" + id).param("scope", "FUTURE"))
                .andExpect(status().isNoContent());

        // 종료일만 당기면 미뤄둔 회차가 되살아난다. 그러면 그만둔 게 아니다
        assertThat(instancesOf(id,
                LocalDate.now().plusDays(1) + "T00:00:00",
                LocalDate.now().plusDays(14) + "T23:59:59")).isEmpty();
    }

    @Test
    @DisplayName("그만둬도 지난 기록은 남는다")
    void stopKeepsPastRecords() throws Exception {
        LocalDate start = LocalDate.now().minusDays(10);
        LocalDate doneOn = LocalDate.now().minusDays(5);
        long id = dailyFrom(start, "지난 습관");
        markDone(id, doneOn.toString());

        mvc.perform(delete(BASE + "/" + id).param("scope", "FUTURE"))
                .andExpect(status().isNoContent());

        List<JsonNode> past = instancesOf(id, start + "T00:00:00", LocalDate.now() + "T23:59:59");

        assertThat(past).isNotEmpty();
        assertThat(fieldAt(past, doneOn.toString(), "status")).isEqualTo("DONE");
    }

    @Test
    @DisplayName("scope를 빠뜨리면 그만두기로 동작해 지난 기록이 살아남는다")
    void deleteDefaultsToStop() throws Exception {
        LocalDate start = LocalDate.now().minusDays(10);
        LocalDate doneOn = LocalDate.now().minusDays(5);
        long id = dailyFrom(start, "기본값 확인");
        markDone(id, doneOn.toString());

        mvc.perform(delete(BASE + "/" + id)).andExpect(status().isNoContent());

        List<JsonNode> past = instancesOf(id, start + "T00:00:00", LocalDate.now() + "T23:59:59");

        assertThat(fieldAt(past, doneOn.toString(), "status"))
                .as("기본값이 ALL이면 과거 기록이 사라진다").isEqualTo("DONE");
    }

    @Test
    @DisplayName("전체 삭제는 지난 기록까지 지운다")
    void deleteAllRemovesEverything() throws Exception {
        LocalDate start = LocalDate.now().minusDays(10);
        long id = dailyFrom(start, "통째로 삭제");
        markDone(id, LocalDate.now().minusDays(5).toString());

        mvc.perform(delete(BASE + "/" + id).param("scope", "ALL"))
                .andExpect(status().isNoContent());

        assertThat(instancesOf(id, start + "T00:00:00", LocalDate.now() + "T23:59:59")).isEmpty();
        mvc.perform(get(BASE + "/" + id)).andExpect(status().isNotFound());
    }

    // --- 헬퍼 -------------------------------------------------------------

    private long daily(String title, String startAt) throws Exception {
        return createSchedule("""
                { "title": "%s", "startAt": "%s",
                  "recurrence": { "freq": "DAILY" } }""".formatted(title, startAt));
    }

    private long dailyFrom(LocalDate start, String title) throws Exception {
        return createSchedule("""
                { "title": "%s", "startAt": "%sT09:00:00",
                  "recurrence": { "freq": "DAILY" } }""".formatted(title, start));
    }

    private long createSchedule(String body) throws Exception {
        JsonNode created = json(mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()));
        return created.get("id").asLong();
    }

    private void markDone(long id, String onDate) throws Exception {
        mvc.perform(patch(BASE + "/" + id + "/instances/" + onDate + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk());
    }

    private List<JsonNode> instancesOf(long scheduleId, String from, String to) throws Exception {
        JsonNode all = json(mvc.perform(get(BASE).param("from", from).param("to", to))
                .andExpect(status().isOk()));
        List<JsonNode> mine = new ArrayList<>();
        for (JsonNode node : all) {
            if (node.get("id").asLong() == scheduleId) {
                mine.add(node);
            }
        }
        return mine;
    }

    private List<String> datesOf(List<JsonNode> nodes) {
        return nodes.stream().map(n -> n.get("instanceDate").asString()).toList();
    }

    private String fieldAt(List<JsonNode> nodes, String instanceDate, String field) {
        return nodes.stream()
                .filter(n -> instanceDate.equals(n.get("instanceDate").asString()))
                .map(n -> n.get(field).asString())
                .findFirst()
                .orElseThrow(() -> new AssertionError("회차가 없다: " + instanceDate));
    }

    private JsonNode json(ResultActions actions) throws Exception {
        return mapper.readTree(actions.andReturn().getResponse().getContentAsString());
    }
}
