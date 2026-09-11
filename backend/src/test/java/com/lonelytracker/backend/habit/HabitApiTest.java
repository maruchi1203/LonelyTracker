package com.lonelytracker.backend.habit;

import com.lonelytracker.backend.habit.repository.HabitLogRepository;
import com.lonelytracker.backend.habit.repository.HabitRepository;
import com.lonelytracker.backend.support.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 습관일지 탭이 쓰는 길을 검증한다.
 * <p>
 * {@code @Transactional} 을 붙이지 않는다. 붙이면 세션이 살아 있어,
 * open-in-view: false 인 실제 서버에서만 나는 지연 로딩 문제를 놓친다.
 */
@AutoConfigureMockMvc
class HabitApiTest extends IntegrationTest {

    private static final String BASE = "/api/habits";

    @Autowired
    MockMvc mvc;

    @Autowired
    HabitRepository habitRepository;

    @Autowired
    HabitLogRepository logRepository;

    @AfterEach
    void clean() {
        // 기록이 습관을 참조한다. 습관을 먼저 지우면 외래 키가 걸린다
        logRepository.deleteAll();
        habitRepository.deleteAll();
    }

    @Test
    @DisplayName("습관을 만들고 목록에서 읽는다")
    void createsAndLists() throws Exception {
        create("{\"title\":\"팔굽혀펴기\",\"category\":\"BODY\""
                + ",\"twoMinuteAction\":\"매트 깔기\"}");

        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("팔굽혀펴기"))
                .andExpect(jsonPath("$[0].category").value("BODY"))
                .andExpect(jsonPath("$[0].twoMinuteAction").value("매트 깔기"))
                .andExpect(jsonPath("$[0].archived").value(false))
                .andExpect(jsonPath("$[0].doneDates.length()").value(0));
    }

    @Test
    @DisplayName("갈래는 필수다")
    void requiresCategory() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"무언가\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("해낸 날을 표시하면 목록에 실린다")
    void marksADay() throws Exception {
        long id = create("{\"title\":\"명상\",\"category\":\"MIND\"}");

        log(id, "2026-09-11", "{\"done\":true,\"note\":\"5분\"}")
                .andExpect(status().isNoContent());

        mvc.perform(get(BASE + "?from=2026-09-01&to=2026-09-30"))
                .andExpect(jsonPath("$[0].doneDates.length()").value(1))
                .andExpect(jsonPath("$[0].doneDates[0]").value("2026-09-11"));
    }

    @Test
    @DisplayName("같은 날을 두 번 표시해도 한 줄이다")
    void keepsOneRowPerDay() throws Exception {
        long id = create("{\"title\":\"명상\",\"category\":\"MIND\"}");

        log(id, "2026-09-11", "{\"done\":true}").andExpect(status().isNoContent());
        log(id, "2026-09-11", "{\"done\":true,\"note\":\"고쳐 적음\"}")
                .andExpect(status().isNoContent());

        mvc.perform(get(BASE + "?from=2026-09-01&to=2026-09-30"))
                .andExpect(jsonPath("$[0].doneDates.length()").value(1));
    }

    @Test
    @DisplayName("되돌리면 그날 기록이 사라진다")
    void undoRemovesTheRow() throws Exception {
        long id = create("{\"title\":\"명상\",\"category\":\"MIND\"}");

        log(id, "2026-09-11", "{\"done\":true}").andExpect(status().isNoContent());
        log(id, "2026-09-11", "{\"done\":false}").andExpect(status().isNoContent());

        mvc.perform(get(BASE + "?from=2026-09-01&to=2026-09-30"))
                .andExpect(jsonPath("$[0].doneDates.length()").value(0));
    }

    @Test
    @DisplayName("한 적 없는 날을 되돌려도 탈이 없다")
    void undoOnAnUntouchedDayIsFine() throws Exception {
        long id = create("{\"title\":\"명상\",\"category\":\"MIND\"}");

        log(id, "2026-09-11", "{\"done\":false}").andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("구간 밖의 기록은 오지 않는다")
    void leavesOutOfWindowLogsOut() throws Exception {
        long id = create("{\"title\":\"명상\",\"category\":\"MIND\"}");

        log(id, "2026-08-01", "{\"done\":true}").andExpect(status().isNoContent());

        mvc.perform(get(BASE + "?from=2026-09-01&to=2026-09-30"))
                .andExpect(jsonPath("$[0].doneDates.length()").value(0));
    }

    @Test
    @DisplayName("그만둬도 기록은 남는다")
    void archiveKeepsTheLogs() throws Exception {
        long id = create("{\"title\":\"명상\",\"category\":\"MIND\"}");
        log(id, "2026-09-11", "{\"done\":true}").andExpect(status().isNoContent());

        mvc.perform(patch(BASE + "/" + id + "/archived?archived=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.archived").value(true));

        mvc.perform(get(BASE + "?from=2026-09-01&to=2026-09-30"))
                .andExpect(jsonPath("$[0].archived").value(true))
                .andExpect(jsonPath("$[0].doneDates.length()").value(1));
    }

    @Test
    @DisplayName("수정하면 갈래도 바뀐다")
    void updatesCategory() throws Exception {
        long id = create("{\"title\":\"명상\",\"category\":\"MIND\"}");

        mvc.perform(put(BASE + "/" + id).contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"필사\",\"category\":\"ART\""
                        + ",\"twoMinuteAction\":\"공책 펴기\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("필사"))
                .andExpect(jsonPath("$.category").value("ART"));
    }

    @Test
    @DisplayName("지우면 기록도 함께 사라진다")
    void deleteTakesTheLogs() throws Exception {
        long id = create("{\"title\":\"명상\",\"category\":\"MIND\"}");
        log(id, "2026-09-11", "{\"done\":true}").andExpect(status().isNoContent());

        mvc.perform(delete(BASE + "/" + id)).andExpect(status().isNoContent());

        mvc.perform(get(BASE)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("없는 습관은 404다")
    void missingHabitIsNotFound() throws Exception {
        mvc.perform(delete(BASE + "/999999")).andExpect(status().isNotFound());
    }

    /** 그날 표시 요청 */
    private org.springframework.test.web.servlet.ResultActions log(
            long id, String onDate, String body) throws Exception {
        return mvc.perform(put(BASE + "/" + id + "/logs/" + onDate)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    /** 습관 하나를 만들고 id 를 돌려준다 */
    private long create(String body) throws Exception {
        return Long.parseLong(mvc.perform(post(BASE)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location")
                .replaceAll(".*/", ""));
    }
}
