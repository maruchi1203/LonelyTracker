package com.lonelytracker.backend.schedule;

import com.lonelytracker.backend.support.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.lonelytracker.backend.schedule.repository.ScheduleRecurRepository;
import com.lonelytracker.backend.schedule.repository.ScheduleRepository;

/**
 * 리스트 탭이 읽는 조회를 검증한다.
 * <p>
 * 다른 API 테스트와 달리 {@code @Transactional} 을 붙이지 않는다.
 * 붙이면 세션이 살아 있어, open-in-view: false 인 실제 서버에서만 나는
 * 지연 로딩 문제를 놓친다. 롤백이 없으므로 만든 것은 직접 지운다.
 */
@AutoConfigureMockMvc
class ScheduleListApiTest extends IntegrationTest {

    private static final String BASE = "/api/schedules";

    @Autowired
    MockMvc mvc;

    @Autowired
    ScheduleRepository scheduleRepository;

    @Autowired
    ScheduleRecurRepository recurRepository;

    @AfterEach
    void clean() {
        // 규칙이 일정을 참조한다. 일정을 먼저 지우면 외래 키가 걸린다
        recurRepository.deleteAll();
        scheduleRepository.deleteAll();
    }

    @Test
    @DisplayName("습관도 리스트에 오고 반복이라고 표시된다")
    void includesHabits() throws Exception {
        create("{\"title\":\"매일 운동\",\"startAt\":\"2026-10-01T07:00:00\""
                + ",\"recurrence\":{\"freq\":\"DAILY\"}}");
        create("{\"title\":\"한 번만\"}");

        // 리스트는 모든 일정을 담는다. 습관만 빼면 화면에서 사라진 것으로 보인다
        mvc.perform(get(BASE + "/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("매일 운동"))
                // 완료를 어느 경로로 보낼지가 이 값에서 갈린다
                .andExpect(jsonPath("$[0].recurring").value(true))
                .andExpect(jsonPath("$[1].recurring").value(false));
    }

    @Test
    @DisplayName("날짜를 안 정한 항목도 태그와 함께 나온다")
    void includesUndatedItemsWithTags() throws Exception {
        create("{\"title\":\"언젠가 할 일\",\"tags\":[\"공부\",\"장기\"]}");

        mvc.perform(get(BASE + "/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].startAt").doesNotExist())
                // 세션이 닫힌 뒤 Jackson 이 읽는다. tagsCopy 를 빼먹으면 여기서 터진다
                .andExpect(jsonPath("$[0].tags.length()").value(2));
    }

    @Test
    @DisplayName("날짜가 순서를 흔들지 않는다")
    void keepsInsertionOrderRegardlessOfDates() throws Exception {
        create("{\"title\":\"A\",\"dueOn\":\"2026-10-05\"}");
        create("{\"title\":\"B\",\"startAt\":\"2026-10-01T09:00:00\"}");
        create("{\"title\":\"C\",\"dueOn\":\"2026-10-03\"}");
        create("{\"title\":\"D\"}");

        // 서버는 사용자가 세운 순서만 지킨다. 날짜로 줄 세우는 것은 화면의 몫이다
        mvc.perform(get(BASE + "/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("A"))
                .andExpect(jsonPath("$[1].title").value("B"))
                .andExpect(jsonPath("$[2].title").value("C"))
                .andExpect(jsonPath("$[3].title").value("D"));
    }

    @Test
    @DisplayName("자식을 거느린 채로 반복이 될 수 있다")
    void turnsAParentIntoARecurringOne() throws Exception {
        long parent = create("{\"title\":\"이사 준비\"}");
        long child = create("{\"title\":\"짐 싸기\",\"parentId\":" + parent + "}");

        mvc.perform(put(BASE + "/" + parent).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"이사 준비\",\"startAt\":\"2026-10-01T09:00:00\""
                                + ",\"recurrence\":{\"freq\":\"DAILY\"}}"))
                .andExpect(status().isOk());

        // 자식이 부모를 잃지 않는다
        mvc.perform(get(BASE + "/" + child))
                .andExpect(jsonPath("$.parentId").value(parent));
    }

    @Test
    @DisplayName("보낸 차례대로 다시 늘어놓는다")
    void reordersSiblings() throws Exception {
        long a = create("{\"title\":\"A\"}");
        long b = create("{\"title\":\"B\"}");
        long c = create("{\"title\":\"C\"}");

        reorder("{\"ids\":[" + c + "," + a + "," + b + "]}")
                .andExpect(status().isNoContent());

        mvc.perform(get(BASE + "/list"))
                .andExpect(jsonPath("$[0].title").value("C"))
                .andExpect(jsonPath("$[1].title").value("A"))
                .andExpect(jsonPath("$[2].title").value("B"))
                // 0부터 다시 매긴다. 사이 값을 쓰지 않아 값이 촘촘해지지 않는다
                .andExpect(jsonPath("$[0].displayOrder").value(0))
                .andExpect(jsonPath("$[2].displayOrder").value(2));
    }

    @Test
    @DisplayName("자식 무리는 따로 매긴다")
    void reordersOneGroupOnly() throws Exception {
        long parent = create("{\"title\":\"이사 준비\"}");
        long first = create("{\"title\":\"짐 싸기\",\"parentId\":" + parent + "}");
        long second = create("{\"title\":\"청소\",\"parentId\":" + parent + "}");

        reorder("{\"parentId\":" + parent + ",\"ids\":[" + second + "," + first + "]}")
                .andExpect(status().isNoContent());

        mvc.perform(get(BASE + "/list"))
                .andExpect(jsonPath("$[0].title").value("이사 준비"))
                .andExpect(jsonPath("$[1].title").value("청소"))
                .andExpect(jsonPath("$[2].title").value("짐 싸기"));
    }

    @Test
    @DisplayName("무리에서 하나라도 빠지면 거절한다")
    void refusesPartialGroup() throws Exception {
        long a = create("{\"title\":\"A\"}");
        create("{\"title\":\"B\"}");

        // 빠진 항목이 어디에 설지 정할 수 없다
        reorder("{\"ids\":[" + a + "]}").andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("밖에 있던 일정을 그 무리로 데려온다")
    void takesAnOutsiderIn() throws Exception {
        long parent = create("{\"title\":\"이사 준비\"}");
        long child = create("{\"title\":\"짐 싸기\",\"parentId\":" + parent + "}");

        // 최상위 무리의 최종 구성원은 둘이다. 자식을 꺼내 올린다
        reorder("{\"ids\":[" + child + "," + parent + "]}")
                .andExpect(status().isNoContent());

        mvc.perform(get(BASE + "/list"))
                .andExpect(jsonPath("$[0].title").value("짐 싸기"))
                .andExpect(jsonPath("$[0].parentId").doesNotExist())
                .andExpect(jsonPath("$[1].title").value("이사 준비"));
    }

    @Test
    @DisplayName("최상위 일정을 다른 일정 밑으로 넣는다")
    void takesAnOutsiderUnderAParent() throws Exception {
        long parent = create("{\"title\":\"이사 준비\"}");
        long child = create("{\"title\":\"짐 싸기\",\"parentId\":" + parent + "}");
        long loner = create("{\"title\":\"청소\"}");

        reorder("{\"parentId\":" + parent + ",\"ids\":[" + loner + "," + child + "]}")
                .andExpect(status().isNoContent());

        mvc.perform(get(BASE + "/" + loner))
                .andExpect(jsonPath("$.parentId").value(parent));
    }

    @Test
    @DisplayName("데려온 일정의 자손이 넘치면 끌어올린다")
    void pullsUpOverflowOfAnIncomer() throws Exception {
        long top = create("{\"title\":\"상위\"}");
        long moving = create("{\"title\":\"옮길 것\"}");
        long child = create("{\"title\":\"자식\",\"parentId\":" + moving + "}");
        long grandChild = create("{\"title\":\"손자\",\"parentId\":" + child + "}");

        // 1단에 앉으면 손자가 4단이 된다. 넘친 손자는 옮긴 것의 자식이 된다
        reorder("{\"parentId\":" + top + ",\"ids\":[" + moving + "]}")
                .andExpect(status().isNoContent());

        mvc.perform(get(BASE + "/" + grandChild))
                .andExpect(jsonPath("$.parentId").value(moving));
    }

    @Test
    @DisplayName("자기 자손의 무리로는 들어갈 수 없다")
    void refusesCycle() throws Exception {
        long parent = create("{\"title\":\"이사 준비\"}");
        long child = create("{\"title\":\"짐 싸기\",\"parentId\":" + parent + "}");

        // 눌러 앉혀서 풀 수 있는 문제가 아니다
        reorder("{\"parentId\":" + child + ",\"ids\":[" + parent + "]}")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("3단보다 깊은 무리는 만들 수 없다")
    void refusesTooDeepGroup() throws Exception {
        long first = create("{\"title\":\"1단\"}");
        long second = create("{\"title\":\"2단\",\"parentId\":" + first + "}");
        long third = create("{\"title\":\"3단\",\"parentId\":" + second + "}");
        long loner = create("{\"title\":\"떠도는 것\"}");

        reorder("{\"parentId\":" + third + ",\"ids\":[" + loner + "]}")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("반복 일정은 데려와도 남의 밑에 서지 않는다")
    void refusesRecurringAsChild() throws Exception {
        long parent = create("{\"title\":\"이사 준비\"}");
        long recurring = create("{\"title\":\"매일 운동\",\"startAt\":\"2026-10-01T07:00:00\""
                + ",\"recurrence\":{\"freq\":\"DAILY\"}}");

        reorder("{\"parentId\":" + parent + ",\"ids\":[" + recurring + "]}")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("반복 일정의 무리로도 데려올 수 있다")
    void takesAnOutsiderUnderARecurringParent() throws Exception {
        long recurring = create("{\"title\":\"매일 운동\",\"startAt\":\"2026-10-01T07:00:00\""
                + ",\"recurrence\":{\"freq\":\"DAILY\"}}");
        long loner = create("{\"title\":\"스트레칭 매트 사기\"}");

        reorder("{\"parentId\":" + recurring + ",\"ids\":[" + loner + "]}")
                .andExpect(status().isNoContent());

        mvc.perform(get(BASE + "/" + loner))
                .andExpect(jsonPath("$.parentId").value(recurring));
    }

    @Test
    @DisplayName("같은 일정을 두 번 보내면 거절한다")
    void refusesDuplicateIds() throws Exception {
        long a = create("{\"title\":\"A\"}");

        reorder("{\"ids\":[" + a + "," + a + "]}")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("습관도 최상위 무리에 낀다")
    void habitsAreSiblings() throws Exception {
        long habit = create("{\"title\":\"매일 운동\",\"startAt\":\"2026-10-01T07:00:00\""
                + ",\"recurrence\":{\"freq\":\"DAILY\"}}");
        long a = create("{\"title\":\"A\"}");
        long b = create("{\"title\":\"B\"}");

        // 리스트가 보는 것과 무리가 같아야 한다. 습관을 빼면 늘 거절당한다
        reorder("{\"ids\":[" + b + "," + a + "]}")
                .andExpect(status().isBadRequest());

        reorder("{\"ids\":[" + b + "," + habit + "," + a + "]}")
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("우선순위를 실어 다니고 수정해도 살아남는다")
    void carriesPriority() throws Exception {
        long id = create("{\"title\":\"보고서\",\"priority\":\"MUST\"}");

        mvc.perform(get(BASE + "/list"))
                .andExpect(jsonPath("$[0].priority").value("MUST"));

        // 수정 요청에서 빠지면 한 번 저장한 뒤 조용히 지워진다
        mvc.perform(put(BASE + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"보고서\",\"priority\":\"MUST\"}"))
                .andExpect(status().isOk());

        mvc.perform(get(BASE + "/" + id))
                .andExpect(jsonPath("$.priority").value("MUST"));
    }

    @Test
    @DisplayName("안 정하면 아무것도 실리지 않는다")
    void leavesPriorityUnset() throws Exception {
        // 기본값을 두지 않아 "아직 안 정함"과 "일부러 Could"가 구분된다
        create("{\"title\":\"그냥 할 일\"}");

        mvc.perform(get(BASE + "/list"))
                .andExpect(jsonPath("$[0].priority").doesNotExist());
    }

    @Test
    @DisplayName("안 하기로 한 일정은 리스트에 남고 달력에서만 빠진다")
    void hidesWontFromTheCalendarOnly() throws Exception {
        create("{\"title\":\"안 하기로 함\",\"startAt\":\"2026-10-01T09:00:00\""
                + ",\"priority\":\"WONT\"}");
        create("{\"title\":\"할 일\",\"startAt\":\"2026-10-01T10:00:00\"}");

        // 지우지 않는다. "안 하기로 했다"는 판단을 기록으로 남긴다
        mvc.perform(get(BASE + "/list"))
                .andExpect(jsonPath("$.length()").value(2));

        // 다만 안 할 일이 시간축을 차지하면 안 된다
        mvc.perform(get(BASE)
                        .param("from", "2026-10-01T00:00:00")
                        .param("to", "2026-10-02T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("할 일"));
    }

    @Test
    @DisplayName("부모를 완료하면 자손도 완료된다")
    void completionRunsDown() throws Exception {
        long parent = create("{\"title\":\"이사 준비\"}");
        long child = create("{\"title\":\"짐 싸기\",\"parentId\":" + parent + "}");
        long grandChild = create("{\"title\":\"박스 사기\",\"parentId\":" + child + "}");

        complete(parent, true).andExpect(status().isOk());

        mvc.perform(get(BASE + "/list"))
                .andExpect(jsonPath("$[?(@.id == " + child + ")].completedAt")
                        .value(org.hamcrest.Matchers.everyItem(
                                org.hamcrest.Matchers.notNullValue())))
                .andExpect(jsonPath("$[?(@.id == " + grandChild + ")].completedAt")
                        .value(org.hamcrest.Matchers.everyItem(
                                org.hamcrest.Matchers.notNullValue())));
    }

    @Test
    @DisplayName("되돌리면 딸려 완료된 자손만 풀린다")
    void undoSparesTheOnesDoneEarlier() throws Exception {
        long parent = create("{\"title\":\"이사 준비\"}");
        long early = create("{\"title\":\"미리 끝낸 일\",\"parentId\":" + parent + "}");
        long late = create("{\"title\":\"같이 끝난 일\",\"parentId\":" + parent + "}");

        // 먼저 끝낸 자식은 완료 시각이 부모와 다르다
        complete(early, true).andExpect(status().isOk());
        complete(parent, true).andExpect(status().isOk());
        complete(parent, false).andExpect(status().isOk());

        mvc.perform(get(BASE + "/list"))
                .andExpect(jsonPath("$[?(@.id == " + early + ")].completedAt")
                        .value(org.hamcrest.Matchers.everyItem(
                                org.hamcrest.Matchers.notNullValue())))
                .andExpect(jsonPath("$[?(@.id == " + late + ")].completedAt")
                        .isEmpty());
    }

    @Test
    @DisplayName("반복 일정은 완료 경로가 다르다")
    void refusesCompletingARecurringOne() throws Exception {
        long recurring = create("{\"title\":\"매일 운동\",\"startAt\":\"2026-10-01T07:00:00\""
                + ",\"recurrence\":{\"freq\":\"DAILY\"}}");

        complete(recurring, true).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("반복 일정은 아직 안 끝낸 가장 빠른 회차를 싣는다")
    void carriesTheCurrentOccurrence() throws Exception {
        long recurring = create("{\"title\":\"매일 운동\",\"startAt\":\"2026-10-01T07:00:00\""
                + ",\"recurrence\":{\"freq\":\"DAILY\"}}");

        mvc.perform(get(BASE + "/list"))
                .andExpect(jsonPath("$[0].occurrenceOn").value("2026-10-01"));

        mvc.perform(patch(BASE + "/" + recurring + "/instances/2026-10-01/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk());

        // 끝낸 회차는 물러나고 다음 것이 올라온다
        mvc.perform(get(BASE + "/list"))
                .andExpect(jsonPath("$[0].occurrenceOn").value("2026-10-02"));
    }

    @Test
    @DisplayName("반복 일정은 규칙도 함께 싣는다")
    void carriesTheRule() throws Exception {
        create("{\"title\":\"운동\",\"startAt\":\"2026-10-05T07:00:00\""
                + ",\"recurrence\":{\"freq\":\"WEEKLY\""
                + ",\"byWeekday\":[\"MONDAY\",\"WEDNESDAY\"]}}");

        mvc.perform(get(BASE + "/list"))
                .andExpect(jsonPath("$[0].recurrence.freq").value("WEEKLY"))
                .andExpect(jsonPath("$[0].recurrence.byWeekday.length()").value(2));
    }

    @Test
    @DisplayName("1회성 일정에는 회차가 실리지 않는다")
    void leavesOccurrenceEmptyForOneOff() throws Exception {
        create("{\"title\":\"보고서\",\"startAt\":\"2026-10-01T09:00:00\"}");

        mvc.perform(get(BASE + "/list"))
                .andExpect(jsonPath("$[0].occurrenceOn").doesNotExist())
                .andExpect(jsonPath("$[0].recurrence").doesNotExist());
    }

    @Test
    @DisplayName("회차가 넘어가면 딸린 자손의 완료가 풀린다")
    void nextOccurrenceReleasesDescendants() throws Exception {
        long recurring = create("{\"title\":\"매일 운동\",\"startAt\":\"2026-10-01T07:00:00\""
                + ",\"recurrence\":{\"freq\":\"DAILY\"}}");
        long child = create("{\"title\":\"운동복 챙기기\",\"parentId\":" + recurring + "}");
        long grandChild = create("{\"title\":\"수건 넣기\",\"parentId\":" + child + "}");

        complete(child, true).andExpect(status().isOk());

        mvc.perform(patch(BASE + "/" + recurring + "/instances/2026-10-01/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isOk());

        // 언제 끝냈는지는 보지 않는다. 회차가 바뀌면 전부 다시 선다
        mvc.perform(get(BASE + "/list"))
                .andExpect(jsonPath("$[?(@.id == " + child + ")].completedAt").isEmpty())
                .andExpect(jsonPath("$[?(@.id == " + grandChild + ")].completedAt").isEmpty());
    }

    /** 완료 여부 바꾸기 요청 */
    private org.springframework.test.web.servlet.ResultActions complete(
            long id, boolean completed) throws Exception {
        return mvc.perform(patch(BASE + "/" + id + "/completion")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"completed\":" + completed + "}"));
    }

    /** 순서 바꾸기 요청 */
    private org.springframework.test.web.servlet.ResultActions reorder(String body)
            throws Exception {
        return mvc.perform(patch(BASE + "/order")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    /** 일정 하나를 만들고 id 를 돌려준다 */
    private long create(String body) throws Exception {
        return Long.parseLong(mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location")
                .replaceAll(".*/", ""));
    }
}
