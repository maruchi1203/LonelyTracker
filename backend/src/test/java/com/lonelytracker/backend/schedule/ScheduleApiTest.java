package com.lonelytracker.backend.schedule;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import com.lonelytracker.backend.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 일정 API 통합 테스트.
 * <p>
 * 컨트롤러부터 실제 PostgreSQL까지 전 구간을 지난다. 서비스만 mock으로 검증하면
 * FK 제약이나 마이그레이션 문제를 잡을 수 없어, 이 프로젝트에서는 통합 테스트가 핵심이다.
 * <p>
 * {@code @Transactional} 이 붙어 테스트마다 롤백되므로 서로 간섭하지 않는다.
 */
@AutoConfigureMockMvc
@Transactional
class ScheduleApiTest extends IntegrationTest {

    private static final String BASE = "/api/schedules";

    /** 조회 조건이 없을 때의 기본 범위가 이번 주 월요일부터 4주간이라 테스트 날짜도 그 안에서 잡는다 */
    private static final LocalDate MONDAY = LocalDate.now().with(DayOfWeek.MONDAY);

    @Autowired
    MockMvc mvc;                // HTTP 요청 모방 (요청 및 응답 반응 확인용)

    @Autowired
    ObjectMapper mapper;        // Jackson의 JSON (역)직렬화용 객체 (앱 설정 사용)

    @Test
    @DisplayName("일정을 생성하면 201과 Location 헤더를 반환한다")
    void create() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(request("회의", "2026-09-01T10:00:00", "2026-09-01T11:00:00")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("회의"))
                .andExpect(jsonPath("$.status").value("PLANNED"))
                .andExpect(jsonPath("$.allDay").value(false));
    }

    @Test
    @DisplayName("allDay를 생략해도 생성된다")
    void createWithoutAllDay() throws Exception {
        // 요청 DTO가 원시 boolean이면 여기서 역직렬화가 깨진다
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(request("운동", "2026-09-01T08:00:00", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.allDay").value(false));
    }

    @Test
    @DisplayName("title이 없으면 400과 안내 메시지를 반환한다")
    void createWithoutTitle() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(request(null, "2026-09-01T10:00:00", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists())
                // devtools가 기본으로 켜는 스택트레이스 노출을 껐는지 함께 검증한다
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("종료가 시작보다 이르면 400을 반환한다")
    void createWithInvertedPeriod() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(request("거꾸로", "2026-09-01T10:00:00", "2026-09-01T09:00:00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("없는 일정을 조회하면 404를 반환한다")
    void findMissing() throws Exception {
        mvc.perform(get(BASE + "/99999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("깨진 JSON은 500이 아니라 400으로 처리된다")
    void createWithBrokenJson() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{\"title\":"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("상태만 변경할 수 있다")
    void changeStatus() throws Exception {
        long id = createAndGetId("완료할 일", "2026-09-02T10:00:00");

        // 상태는 회차의 것이다. 단일 일정도 "1회짜리 일정" 이라 회차 경로를 쓴다.
        mvc.perform(patch(BASE + "/" + id + "/instances/2026-09-02/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));
    }

    @Test
    @DisplayName("정의되지 않은 상태값은 400을 반환한다")
    void changeStatusWithUnknownValue() throws Exception {
        long id = createAndGetId("상태 테스트", "2026-09-02T10:00:00");

        mvc.perform(patch(BASE + "/" + id + "/instances/2026-09-02/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"NOPE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("수정 응답의 updatedAt이 즉시 조회 결과와 일치한다")
    void updateRefreshesUpdatedAt() throws Exception {
        // @LastModifiedDate는 flush 시점에 채워진다.
        // flush 전에 DTO를 만들면 수정 전 updatedAt이 응답에 실린다.
        long id = createAndGetId("원래 제목", "2026-09-03T10:00:00");

        JsonNode updated = json(mvc.perform(put(BASE + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(request("바뀐 제목", "2026-09-03T11:00:00", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("바뀐 제목")));

        JsonNode fetched = json(mvc.perform(get(BASE + "/" + id)).andExpect(status().isOk()));

        assertThat(updated.get("updatedAt").asString())
                .as("수정 응답의 updatedAt이 실제 저장값과 달라졌다 (flush 시점 문제)")
                .isEqualTo(fetched.get("updatedAt").asString());
    }

    @Test
    @DisplayName("삭제하면 204를 반환하고 다시 조회하면 404가 된다")
    void deleteSchedule() throws Exception {
        long id = createAndGetId("지울 일정", "2026-09-04T10:00:00");

        mvc.perform(delete(BASE + "/" + id)).andExpect(status().isNoContent());
        mvc.perform(get(BASE + "/" + id)).andExpect(status().isNotFound());
        mvc.perform(delete(BASE + "/" + id)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("기간으로 거르면 그 구간에 시작하는 일정만 나온다")
    void searchByPeriod() throws Exception {
        createAndGetId("오전 일정", "2026-09-10T09:00:00");
        createAndGetId("오후 일정", "2026-09-10T15:00:00");

        JsonNode found = json(mvc.perform(get(BASE)
                        .param("from", "2026-09-10T00:00:00")
                        .param("to", "2026-09-10T12:00:00"))
                .andExpect(status().isOk()));

        assertThat(titlesOf(found)).contains("오전 일정").doesNotContain("오후 일정");
    }

    @Test
    @DisplayName("상태로 거를 수 있다")
    void searchByStatus() throws Exception {
        long done = createAndGetId("끝낸 일정", inWindow(2, "09:00:00"));
        createAndGetId("남은 일정", inWindow(2, "10:00:00"));

        mvc.perform(patch(BASE + "/" + done + "/instances/" + MONDAY.plusDays(2) + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"DONE\"}"));

        JsonNode found = json(mvc.perform(get(BASE).param("status", "DONE")).andExpect(status().isOk()));

        assertThat(titlesOf(found)).contains("끝낸 일정").doesNotContain("남은 일정");
    }

    @Test
    @DisplayName("조건을 주지 않으면 시작 시각 오름차순으로 나온다")
    void searchIsSortedByStartAt() throws Exception {
        createAndGetId("나중 일정", inWindow(9, "18:00:00"));
        createAndGetId("먼저 일정", inWindow(9, "07:00:00"));

        JsonNode found = json(mvc.perform(get(BASE)).andExpect(status().isOk()));
        var titles = titlesOf(found);

        assertThat(titles.indexOf("먼저 일정")).isLessThan(titles.indexOf("나중 일정"));
    }

    @Test
    @DisplayName("조건을 주지 않으면 이번 주 월요일부터 나온다")
    void searchDefaultsToThisWeek() throws Exception {
        createAndGetId("월요일 일정", inWindow(0, "09:00:00"));
        createAndGetId("지난주 일요일 일정", MONDAY.minusDays(1) + "T09:00:00");

        JsonNode found = json(mvc.perform(get(BASE)).andExpect(status().isOk()));

        assertThat(titlesOf(found)).contains("월요일 일정").doesNotContain("지난주 일요일 일정");
    }

    @Test
    @DisplayName("기본 범위는 4주째 일요일 밤까지 포함한다")
    void searchDefaultCoversLastSundayNight() throws Exception {
        createAndGetId("마지막 일요일 밤 일정", MONDAY.plusWeeks(4).minusDays(1) + "T23:30:00");
        createAndGetId("범위 밖 일정", MONDAY.plusWeeks(4) + "T09:00:00");

        JsonNode found = json(mvc.perform(get(BASE)).andExpect(status().isOk()));

        assertThat(titlesOf(found)).contains("마지막 일요일 밤 일정").doesNotContain("범위 밖 일정");
    }

    @Test
    @DisplayName("일정 하나가 366일을 넘으면 400을 반환한다")
    void createWithTooLongPeriod() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(request("긴 일정", "2026-01-01T00:00:00", "2028-01-01T00:00:00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("366일")));
    }

    @Test
    @DisplayName("조회 구간이 뒤집히면 400을 반환한다")
    void searchWithInvertedWindow() throws Exception {
        mvc.perform(get(BASE)
                        .param("from", "2026-09-10T00:00:00")
                        .param("to", "2026-09-01T00:00:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("조회 구간이 366일을 넘으면 400을 반환한다")
    void searchWithTooWideWindow() throws Exception {
        mvc.perform(get(BASE)
                        .param("from", "2020-01-01T00:00:00")
                        .param("to", "2030-01-01T00:00:00"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("장소와 2분 행동은 일정 단위 값이라 어느 회차를 봐도 같다")
    void placeAndTwoMinuteActionAreTheSameOnEveryInstance() throws Exception {
        ObjectNode node = mapper.createObjectNode();
        node.put("title", "운동");
        node.put("startAt", inWindow(1, "07:00:00"));
        node.put("place", "헬스장");
        node.put("twoMinuteAction", "운동복 갈아입기");
        node.putObject("recurrence").put("freq", "DAILY");

        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(node)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.place").value("헬스장"))
                .andExpect(jsonPath("$.twoMinuteAction").value("운동복 갈아입기"));

        JsonNode instances = json(mvc.perform(get(BASE)).andExpect(status().isOk()));

        assertThat(instances).hasSizeGreaterThan(1);
        assertThat(instances).allSatisfy(instance -> {
            assertThat(instance.get("place").asString()).isEqualTo("헬스장");
            assertThat(instance.get("twoMinuteAction").asString()).isEqualTo("운동복 갈아입기");
        });
    }

    @Test
    @DisplayName("단건 조회 응답을 그대로 되돌려 보내도 장소와 2분 행동이 남는다")
    void detailResponseRoundTripsPlaceAndTwoMinuteAction() throws Exception {
        ObjectNode node = mapper.createObjectNode();
        node.put("title", "운동");
        node.put("startAt", inWindow(1, "07:00:00"));
        node.put("place", "헬스장");
        node.put("twoMinuteAction", "운동복 갈아입기");

        long id = json(mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(node)))
                .andExpect(status().isCreated()))
                .get("id").asLong();

        ObjectNode detail = (ObjectNode) json(mvc.perform(get(BASE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.place").value("헬스장"))
                .andExpect(jsonPath("$.twoMinuteAction").value("운동복 갈아입기")));

        // 읽기 전용 셋만 떼면 그대로 수정 요청 본문이 된다
        detail.remove("id");
        detail.remove("createdAt");
        detail.remove("updatedAt");

        mvc.perform(put(BASE + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(detail)))
                .andExpect(status().isOk());

        mvc.perform(get(BASE + "/" + id))
                .andExpect(jsonPath("$.place").value("헬스장"))
                .andExpect(jsonPath("$.twoMinuteAction").value("운동복 갈아입기"));
    }

    @Test
    @DisplayName("태그를 여러 개 붙일 수 있고 그중 하나로 걸러진다")
    void tagsAreManyAndFilterable() throws Exception {
        ObjectNode node = mapper.createObjectNode();
        node.put("title", "운동");
        node.put("startAt", inWindow(1, "07:00:00"));
        node.putArray("tags").add("육체").add("아침");

        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(node)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tags.length()").value(2));

        createAndGetId("독서", inWindow(1, "21:00:00"));

        JsonNode found = json(mvc.perform(get(BASE).param("tag", "아침"))
                .andExpect(status().isOk()));

        assertThat(found).hasSize(1);
        assertThat(found.get(0).get("title").asString()).isEqualTo("운동");
    }

    @Test
    @DisplayName("빈 태그와 앞뒤 공백은 버리고 중복은 하나로 합친다")
    void tagsAreNormalized() throws Exception {
        ObjectNode node = mapper.createObjectNode();
        node.put("title", "운동");
        node.put("startAt", inWindow(1, "07:00:00"));
        node.putArray("tags").add("  육체  ").add("육체").add("   ");

        JsonNode created = json(mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(node)))
                .andExpect(status().isCreated()));

        assertThat(created.get("tags")).hasSize(1);
        assertThat(created.get("tags").get(0).asString()).isEqualTo("육체");
    }

    @Test
    @DisplayName("태그 목록은 이미 쓴 이름만 중복 없이 정렬해 돌려준다")
    void listsUsedTagNames() throws Exception {
        ObjectNode first = mapper.createObjectNode();
        first.put("title", "운동");
        first.put("startAt", inWindow(1, "07:00:00"));
        first.putArray("tags").add("육체").add("아침");

        ObjectNode second = mapper.createObjectNode();
        second.put("title", "달리기");
        second.put("startAt", inWindow(2, "07:00:00"));
        second.putArray("tags").add("육체");

        for (ObjectNode body : List.of(first, second)) {
            mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(body)))
                    .andExpect(status().isCreated());
        }

        // 두 일정이 "육체"를 함께 쓰지만 후보로는 한 번만 나온다
        mvc.perform(get(BASE + "/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("아침"))
                .andExpect(jsonPath("$[1]").value("육체"));
    }

    // --- 헬퍼 -------------------------------------------------------------

    /** 이번 주 월요일로부터 며칠 뒤의 시각을 요청용 문자열로 만든다. */
    private String inWindow(int plusDays, String time) {
        return MONDAY.plusDays(plusDays) + "T" + time;
    }

    /** title이 null이면 필드 자체를 빼서 검증 실패 상황을 만든다. */
    private String request(String title, String startAt, String endAt) throws Exception {
        ObjectNode node = mapper.createObjectNode();
        if (title != null) {
            node.put("title", title);
        }
        node.put("startAt", startAt);
        if (endAt != null) {
            node.put("endAt", endAt);
        }
        return mapper.writeValueAsString(node);
    }

    private long createAndGetId(String title, String startAt) throws Exception {
        JsonNode created = json(mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(request(title, startAt, null)))
                .andExpect(status().isCreated()));
        return created.get("id").asLong();
    }

    private JsonNode json(org.springframework.test.web.servlet.ResultActions actions) throws Exception {
        return mapper.readTree(actions.andReturn().getResponse().getContentAsString());
    }

    private java.util.List<String> titlesOf(JsonNode array) {
        java.util.List<String> titles = new java.util.ArrayList<>();
        array.forEach(node -> titles.add(node.get("title").asString()));
        return titles;
    }
}
