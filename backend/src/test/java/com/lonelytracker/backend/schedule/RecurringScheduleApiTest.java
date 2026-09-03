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
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 반복 일정 생성과 조회 전개.
 * <p>
 * 회차는 행으로 존재하지 않는다. 조회할 때마다 규칙에서 펼쳐진다.
 * 그래서 "몇 행 생겼나" 가 아니라 <b>"조회하면 어떤 날짜가 나오나"</b> 가 검증 대상이다.
 * <p>
 * 단일 일정도 "1회짜리 일정" 이라 같은 엔드포인트를 쓴다.
 * recurrence 를 주면 반복, 안 주면 1회성이다.
 */
@AutoConfigureMockMvc
@Transactional
class RecurringScheduleApiTest extends IntegrationTest {

    private static final String BASE = "/api/schedules";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @Test
    @DisplayName("recurrence를 주면 반복 일정이 되고 첫 회차를 돌려준다")
    void createsRecurringSchedule() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
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
                // 반복이든 아니든 id 가 있다. 회차 식별자는 id + instanceDate 다
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.instanceDate").value("2026-09-07"))
                .andExpect(jsonPath("$.title").value("운동"))
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.recurring").value(true))
                .andExpect(jsonPath("$.postponeCount").value(0));
    }

    @Test
    @DisplayName("recurrence를 안 주면 그 날짜에만 회차가 하나 나온다")
    void createsSingleSchedule() throws Exception {
        long id = createSchedule("""
                { "title": "혼자", "startAt": "2026-09-08T09:00:00" }""");

        List<JsonNode> month = instancesOf(id, "2026-09-01T00:00:00", "2026-09-30T23:59:59");

        assertThat(datesOf(month)).containsExactly("2026-09-08");
    }

    @Test
    @DisplayName("회차마다 반복 일정인지 알려준다")
    void tellsWhetherTheInstanceRepeats() throws Exception {
        // 규칙 자체는 응답에 없다. 화면이 반복과 1회성을 나눠 보여주려면 이 값이 필요하다
        long weekly = createSchedule("""
                {
                  "title": "운동",
                  "startAt": "2026-09-07T07:00:00",
                  "recurrence": { "freq": "WEEKLY", "byWeekday": ["MONDAY"] }
                }""");
        long once = createSchedule("""
                { "title": "혼자", "startAt": "2026-09-08T09:00:00" }""");

        String window = "2026-09-01T00:00:00";
        String end = "2026-09-30T23:59:59";

        assertThat(instancesOf(weekly, window, end))
                .isNotEmpty()
                .allSatisfy(o -> assertThat(o.path("recurring").asBoolean()).isTrue());
        assertThat(instancesOf(once, window, end))
                .isNotEmpty()
                .allSatisfy(o -> assertThat(o.path("recurring").asBoolean()).isFalse());
    }

    @Test
    @DisplayName("범위 밖에서 미뤄져 들어온 회차도 반복 여부를 유지한다")
    void keepsRecurringOnPostponedInstance() throws Exception {
        long id = createSchedule("""
                {
                  "title": "운동",
                  "startAt": "2026-09-07T07:00:00",
                  "recurrence": { "freq": "WEEKLY", "byWeekday": ["MONDAY"] }
                }""");

        // 규칙은 월요일뿐이라 수요일(10/7)에는 이 회차만 잡힌다.
        // onDate 는 9/7 그대로여서 첫 루프가 아니라 두 번째 루프가 넣는다
        mvc.perform(patch(BASE + "/" + id + "/instances/2026-09-07/postpone")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "to": "2026-10-07T07:00:00" }"""))
                .andExpect(status().isOk());

        assertThat(instancesOf(id, "2026-10-06T00:00:00", "2026-10-08T23:59:59"))
                .isNotEmpty()
                .allSatisfy(o -> assertThat(o.path("recurring").asBoolean()).isTrue());
    }

    @Test
    @DisplayName("조회하면 규칙대로 그 요일에만 회차가 나온다")
    void expandsOnRequestedWeekdays() throws Exception {
        long id = createSchedule("""
                { "title": "운동", "startAt": "2026-09-07T07:00:00",
                  "recurrence": { "freq": "WEEKLY", "byWeekday": ["MONDAY", "WEDNESDAY", "FRIDAY"] } }""");

        List<JsonNode> week = instancesOf(id, "2026-09-07T00:00:00", "2026-09-13T23:59:59");

        assertThat(datesOf(week)).containsExactly("2026-09-07", "2026-09-09", "2026-09-11");
    }

    @Test
    @DisplayName("모든 회차가 일정의 시각과 소요시간을 물려받는다")
    void inheritsTimeAndDuration() throws Exception {
        long id = createSchedule("""
                { "title": "아침 운동", "startAt": "2026-09-07T07:00:00",
                  "endAt": "2026-09-07T08:30:00",
                  "recurrence": { "freq": "WEEKLY", "byWeekday": ["MONDAY"] } }""");

        List<JsonNode> month = instancesOf(id, "2026-09-01T00:00:00", "2026-09-30T23:59:59");

        assertThat(month).isNotEmpty();
        month.forEach(node -> {
            assertThat(node.get("startAt").asString()).endsWith("T07:00:00");
            assertThat(node.get("endAt").asString()).endsWith("T08:30:00");
        });
    }

    @Test
    @DisplayName("정각이 아닌 시각도 분 단위까지 그대로 유지된다")
    void keepsMinutePrecision() throws Exception {
        // 테스트가 전부 정각이면 분 단위가 잘리는 버그를 못 잡는다
        long id = createSchedule("""
                { "title": "점심 산책", "startAt": "2026-09-07T12:30:00",
                  "endAt": "2026-09-07T13:15:00",
                  "recurrence": { "freq": "DAILY", "endsOn": "2026-09-09" } }""");

        List<JsonNode> found = instancesOf(id, "2026-09-07T00:00:00", "2026-09-09T23:59:59");

        assertThat(found).hasSize(3);
        found.forEach(node -> {
            assertThat(node.get("startAt").asString()).endsWith("T12:30:00");
            assertThat(node.get("endAt").asString()).endsWith("T13:15:00");
        });
    }

    @Test
    @DisplayName("종료 시각이 없으면 회차에도 없다")
    void noEndAtWhenDurationAbsent() throws Exception {
        long id = createSchedule("""
                { "title": "독서", "startAt": "2026-09-01T21:00:00",
                  "recurrence": { "freq": "DAILY", "endsOn": "2026-09-02" } }""");

        List<JsonNode> found = instancesOf(id, "2026-09-01T00:00:00", "2026-09-02T23:59:59");

        assertThat(found).hasSize(2);
        // non_null 직렬화라 값이 없으면 필드 자체가 빠진다
        found.forEach(node -> assertThat(node.get("endAt")).isNull());
    }

    @Test
    @DisplayName("무기한 반복도 조회 범위만큼만 나온다")
    void openEndedIsBoundedByQuery() throws Exception {
        // 미리 만들었다면 애초에 쓸 수 없는 테스트다. 무한히 만들 수 없으니까.
        long id = createSchedule("""
                { "title": "매일 독서", "startAt": "2026-09-01T21:00:00",
                  "recurrence": { "freq": "DAILY" } }""");

        assertThat(instancesOf(id, "2026-09-01T00:00:00", "2026-09-07T23:59:59")).hasSize(7);
        assertThat(instancesOf(id, "2026-09-01T00:00:00", "2026-09-30T23:59:59")).hasSize(30);
    }

    @Test
    @DisplayName("종료일 이후는 나오지 않는다")
    void respectsEndDate() throws Exception {
        long id = createSchedule("""
                { "title": "짧은 반복", "startAt": "2026-09-01T09:00:00",
                  "recurrence": { "freq": "DAILY", "endsOn": "2026-09-05" } }""");

        List<JsonNode> found = instancesOf(id, "2026-09-01T00:00:00", "2026-09-30T23:59:59");

        assertThat(datesOf(found)).containsExactly(
                "2026-09-01", "2026-09-02", "2026-09-03", "2026-09-04", "2026-09-05");
    }

    @Test
    @DisplayName("시작일 이전은 나오지 않는다")
    void respectsStartDate() throws Exception {
        long id = createSchedule("""
                { "title": "나중 시작", "startAt": "2026-09-15T09:00:00",
                  "recurrence": { "freq": "DAILY", "endsOn": "2026-09-17" } }""");

        List<JsonNode> found = instancesOf(id, "2026-09-01T00:00:00", "2026-09-30T23:59:59");

        assertThat(datesOf(found)).containsExactly("2026-09-15", "2026-09-16", "2026-09-17");
    }

    @Test
    @DisplayName("분류로 거르면 반복 회차도 함께 걸린다")
    void filtersByCategory() throws Exception {
        createSchedule("""
                { "title": "운동", "startAt": "2026-09-01T07:00:00", "category": "육체",
                  "recurrence": { "freq": "DAILY", "endsOn": "2026-09-03" } }""");
        createSchedule("""
                { "title": "독서", "startAt": "2026-09-01T21:00:00", "category": "정신",
                  "recurrence": { "freq": "DAILY", "endsOn": "2026-09-03" } }""");

        JsonNode found = json(mvc.perform(get(BASE)
                        .param("from", "2026-09-01T00:00:00")
                        .param("to", "2026-09-03T23:59:59")
                        .param("category", "육체"))
                .andExpect(status().isOk()));

        assertThat(titlesOf(found)).contains("운동").doesNotContain("독서");
    }

    @Test
    @DisplayName("매주인데 요일을 안 고르면 400을 반환한다")
    void rejectsWeeklyWithoutWeekday() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "요일 없음", "startAt": "2026-09-07T09:00:00",
                                  "recurrence": { "freq": "WEEKLY", "byWeekday": [] } }"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("freq가 없으면 400을 반환한다")
    void rejectsRecurrenceWithoutFreq() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "빈도 없음", "startAt": "2026-09-07T09:00:00",
                                  "recurrence": { "endsOn": "2026-09-10" } }"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("종료일이 시작일보다 이르면 400을 반환한다")
    void rejectsInvertedRecurrenceRange() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "거꾸로", "startAt": "2026-09-10T09:00:00",
                                  "recurrence": { "freq": "DAILY", "endsOn": "2026-09-01" } }"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("자정을 넘겨도 다음 회차 전에 끝나면 반복할 수 있다")
    void overnightRecurringIsAllowed() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "야간 근무", "startAt": "2026-09-01T22:00:00",
                                  "endAt": "2026-09-02T02:00:00",
                                  "recurrence": { "freq": "DAILY" } }"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.endAt").value("2026-09-02T02:00:00"));
    }

    @Test
    @DisplayName("매일 반복이 24시간을 넘으면 400을 반환한다")
    void dailyLongerThanADayIsRejected() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "겹치는 일정", "startAt": "2026-09-01T22:00:00",
                                  "endAt": "2026-09-02T23:00:00",
                                  "recurrence": { "freq": "DAILY" } }"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("다음 회차")));
    }

    @Test
    @DisplayName("매주 월수금은 회차 사이 간격인 48시간까지 이어질 수 있다")
    void weeklyGapAllowsUpToTheNextInstance() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "합숙", "startAt": "2026-09-07T09:00:00",
                                  "endAt": "2026-09-09T09:00:00",
                                  "recurrence": { "freq": "WEEKLY",
                                                  "byWeekday": ["MONDAY", "WEDNESDAY", "FRIDAY"] } }"""))
                .andExpect(status().isCreated());

        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "너무 긴 합숙", "startAt": "2026-09-07T09:00:00",
                                  "endAt": "2026-09-09T10:00:00",
                                  "recurrence": { "freq": "WEEKLY",
                                                  "byWeekday": ["MONDAY", "WEDNESDAY", "FRIDAY"] } }"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("일정 하나를 조회하면 반복 규칙이 함께 온다")
    void detailCarriesTheRecurrenceRule() throws Exception {
        long id = createSchedule("""
                { "title": "운동", "startAt": "2026-09-07T07:00:00",
                  "endAt": "2026-09-07T08:00:00",
                  "recurrence": { "freq": "WEEKLY",
                                  "byWeekday": ["MONDAY", "FRIDAY"],
                                  "endsOn": "2026-12-31" } }""");

        mvc.perform(get(BASE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startAt").value("2026-09-07T07:00:00"))
                // 요청과 같은 모양이라 그대로 되돌려 보낼 수 있다
                .andExpect(jsonPath("$.endAt").value("2026-09-07T08:00:00"))
                .andExpect(jsonPath("$.recurrence.freq").value("WEEKLY"))
                .andExpect(jsonPath("$.recurrence.endsOn").value("2026-12-31"))
                .andExpect(jsonPath("$.recurrence.byWeekday",
                        org.hamcrest.Matchers.containsInAnyOrder("MONDAY", "FRIDAY")));
    }

    @Test
    @DisplayName("조회한 그대로 되돌려 보내면 아무것도 바뀌지 않는다")
    void detailCanBeSentBackUnchanged() throws Exception {
        long id = createSchedule("""
                { "title": "운동", "startAt": "2026-09-07T07:00:00",
                  "endAt": "2026-09-07T08:00:00", "category": "육체",
                  "recurrence": { "freq": "WEEKLY",
                                  "byWeekday": ["MONDAY", "FRIDAY"],
                                  "endsOn": "2026-12-31" } }""");

        JsonNode before = json(mvc.perform(get(BASE + "/" + id)).andExpect(status().isOk()));

        // 읽기 전용 셋만 떼고 그대로 PUT 한다
        ObjectNode body = (ObjectNode) before.deepCopy();
        body.remove("id");
        body.remove("createdAt");
        body.remove("updatedAt");
        mvc.perform(put(BASE + "/" + id).contentType(MediaType.APPLICATION_JSON)
                .content(body.toString())).andExpect(status().isOk());

        JsonNode after = json(mvc.perform(get(BASE + "/" + id)).andExpect(status().isOk()));

        assertThat(after.get("startAt")).isEqualTo(before.get("startAt"));
        assertThat(after.get("endAt")).isEqualTo(before.get("endAt"));
        assertThat(after.get("category")).isEqualTo(before.get("category"));
        assertThat(after.get("recurrence")).as("반복이 사라지면 안 된다")
                .isEqualTo(before.get("recurrence"));
    }

    @Test
    @DisplayName("1회성 일정은 recurrence가 null이다")
    void detailOfSingleScheduleHasNoRule() throws Exception {
        long id = createSchedule("""
                { "title": "회의", "startAt": "2026-09-07T10:00:00" }""");

        mvc.perform(get(BASE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recurrence").doesNotExist())
                .andExpect(jsonPath("$.endAt").doesNotExist());
    }

    // --- 헬퍼 -------------------------------------------------------------

    private long createSchedule(String body) throws Exception {
        JsonNode created = json(mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()));
        return created.get("id").asLong();
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

    private List<String> titlesOf(JsonNode array) {
        List<String> titles = new ArrayList<>();
        array.forEach(n -> titles.add(n.get("title").asString()));
        return titles;
    }

    private JsonNode json(ResultActions actions) throws Exception {
        return mapper.readTree(actions.andReturn().getResponse().getContentAsString());
    }
}
