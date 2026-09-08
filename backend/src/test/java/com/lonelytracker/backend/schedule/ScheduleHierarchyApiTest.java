package com.lonelytracker.backend.schedule;

import com.lonelytracker.backend.support.IntegrationTest;
import com.lonelytracker.backend.user.entity.UserEntity;
import com.lonelytracker.backend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.lonelytracker.backend.schedule.entity.ScheduleEntity;
import com.lonelytracker.backend.schedule.repository.ScheduleRepository;

/**
 * 일정을 다른 일정 밑에 거는 규칙을 검증한다.
 * <p>
 * 깊이는 3단까지다. 넘치면 거부하지 않고 들어갈 수 있는 자리로 눌러 앉힌다.
 * 습관은 계층에 끼지 않고, 부모를 지워도 자식은 남는다.
 * 규칙을 서비스 계층이 지키므로 DB 제약으로는 증명할 수 없다.
 */
@AutoConfigureMockMvc
@Transactional
class ScheduleHierarchyApiTest extends IntegrationTest {

    private static final String BASE = "/api/schedules";

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ScheduleRepository scheduleRepository;

    @Test
    @DisplayName("상위 일정과 기한이 그대로 돌아온다")
    void keepsParentAndDueDate() throws Exception {
        long parent = create("{\"title\":\"이사 준비\"}");
        long child = create("{\"title\":\"짐 싸기\",\"parentId\":" + parent
                + ",\"dueOn\":\"2026-10-01\"}");

        mvc.perform(get(BASE + "/" + child))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentId").value(parent))
                .andExpect(jsonPath("$.dueOn").value("2026-10-01"));

        // 수정 폼이 읽은 값을 그대로 돌려보내도 살아남는다
        // 수정 요청에서 빠지면 한 번 저장한 뒤 조용히 지워진다
        mvc.perform(put(BASE + "/" + child).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"짐 싸기\",\"parentId\":" + parent
                                + ",\"dueOn\":\"2026-10-01\"}"))
                .andExpect(status().isOk());

        mvc.perform(get(BASE + "/" + child))
                .andExpect(jsonPath("$.parentId").value(parent))
                .andExpect(jsonPath("$.dueOn").value("2026-10-01"));
    }

    @Test
    @DisplayName("3단 밑에 걸면 2단 자리로 눌러 앉는다")
    void clampsWhenParentIsTooDeep() throws Exception {
        long root = create("{\"title\":\"1단\"}");
        long second = create("{\"title\":\"2단\",\"parentId\":" + root + "}");
        long third = create("{\"title\":\"3단\",\"parentId\":" + second + "}");

        // 3단 밑은 자리가 없다. 거부하지 않고 그 위의 2단 항목에 붙인다
        long fourth = create("{\"title\":\"4단이 될 뻔\",\"parentId\":" + third + "}");

        mvc.perform(get(BASE + "/" + fourth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentId").value(second));
    }

    @Test
    @DisplayName("옮기면서 넘친 손자가 끌어올려진다")
    void pullsUpOverflowingGrandChildren() throws Exception {
        long other = create("{\"title\":\"다른 최상위\"}");
        long root = create("{\"title\":\"1단\"}");
        long second = create("{\"title\":\"2단\",\"parentId\":" + root + "}");
        long third = create("{\"title\":\"3단\",\"parentId\":" + second + "}");

        // root 가 1단으로 내려가면 third 는 3단이 된다
        mvc.perform(put(BASE + "/" + root).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"1단\",\"parentId\":" + other + "}"))
                .andExpect(status().isOk());

        // third 는 자기 위쪽에서 깊이 1인 root 의 자식이 된다
        mvc.perform(get(BASE + "/" + third))
                .andExpect(jsonPath("$.parentId").value(root));
        mvc.perform(get(BASE + "/" + second))
                .andExpect(jsonPath("$.parentId").value(root));
    }

    @Test
    @DisplayName("2단으로 옮기면 자식까지 형제로 올라온다")
    void pullsUpOverflowingChildren() throws Exception {
        long a = create("{\"title\":\"A\"}");
        long b = create("{\"title\":\"B\",\"parentId\":" + a + "}");
        long x = create("{\"title\":\"X\"}");
        long y = create("{\"title\":\"Y\",\"parentId\":" + x + "}");

        // X 가 2단에 앉으면 Y 는 3단이 된다
        mvc.perform(put(BASE + "/" + x).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"X\",\"parentId\":" + b + "}"))
                .andExpect(status().isOk());

        // Y 는 깊이 1인 B 의 자식, 곧 X 의 형제가 된다
        mvc.perform(get(BASE + "/" + y))
                .andExpect(jsonPath("$.parentId").value(b));
        mvc.perform(get(BASE + "/" + x))
                .andExpect(jsonPath("$.parentId").value(b));
    }

    @Test
    @DisplayName("자기 자신을 상위로 둘 수 없다")
    void rejectsSelfAsParent() throws Exception {
        long id = create("{\"title\":\"혼자\"}");

        mvc.perform(put(BASE + "/" + id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"혼자\",\"parentId\":" + id + "}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("서로를 상위로 가리킬 수 없다")
    void rejectsCycle() throws Exception {
        long a = create("{\"title\":\"A\"}");
        long b = create("{\"title\":\"B\",\"parentId\":" + a + "}");

        mvc.perform(put(BASE + "/" + a).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"A\",\"parentId\":" + b + "}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("습관은 상위 일정을 가질 수 없다")
    void rejectsParentOnRecurring() throws Exception {
        long parent = create("{\"title\":\"묶음\"}");

        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"매일 운동\",\"startAt\":\"2026-10-01T07:00:00\""
                                + ",\"parentId\":" + parent
                                + ",\"recurrence\":{\"freq\":\"DAILY\"}}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("습관은 다른 일정을 거느릴 수 없다")
    void rejectsRecurringAsParent() throws Exception {
        long habit = create("{\"title\":\"매일 운동\",\"startAt\":\"2026-10-01T07:00:00\""
                + ",\"recurrence\":{\"freq\":\"DAILY\"}}");

        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"딸린 일정\",\"parentId\":" + habit + "}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("남의 일정을 상위로 걸면 404가 된다")
    void hidesOtherUsersScheduleAsParent() throws Exception {
        UserEntity other = userRepository.save(UserEntity.builder()
                .username("someone-else")
                .displayName("남의 계정")
                .build());
        Long theirs = scheduleRepository.save(ScheduleEntity.builder()
                .user(other)
                .title("남의 일정")
                .build()).getId();

        // 400 을 내면 그 id 의 일정이 있다는 사실이 새어 나간다
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"붙이기 시도\",\"parentId\":" + theirs + "}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("상위를 지우면 자식은 남아 최상위가 된다")
    void keepsChildrenWhenParentGoes() throws Exception {
        long parent = create("{\"title\":\"이사 준비\"}");
        long child = create("{\"title\":\"짐 싸기\",\"parentId\":" + parent + "}");

        mvc.perform(delete(BASE + "/" + parent)).andExpect(status().isNoContent());

        mvc.perform(get(BASE + "/" + child))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentId").doesNotExist());
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
