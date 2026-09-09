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
    @DisplayName("자식이 있는 일정은 습관으로 바꿀 수 없다")
    void refusesTurningAParentIntoAHabit() throws Exception {
        long parent = create("{\"title\":\"이사 준비\"}");
        create("{\"title\":\"짐 싸기\",\"parentId\":" + parent + "}");

        // 습관은 자식을 거느리지 않는다
        mvc.perform(put(BASE + "/" + parent).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"이사 준비\",\"startAt\":\"2026-10-01T09:00:00\""
                                + ",\"recurrence\":{\"freq\":\"DAILY\"}}"))
                .andExpect(status().isBadRequest());
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
    @DisplayName("다른 무리의 일정을 섞으면 거절한다")
    void refusesOutsider() throws Exception {
        long parent = create("{\"title\":\"이사 준비\"}");
        long child = create("{\"title\":\"짐 싸기\",\"parentId\":" + parent + "}");

        reorder("{\"ids\":[" + parent + "," + child + "]}")
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
