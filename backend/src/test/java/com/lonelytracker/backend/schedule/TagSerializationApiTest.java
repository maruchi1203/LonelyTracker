package com.lonelytracker.backend.schedule;

import com.lonelytracker.backend.schedule.repository.ScheduleRepository;
import com.lonelytracker.backend.support.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 태그가 응답으로 직렬화되는지 본다
 * <p>
 * 다른 API 테스트와 달리 {@code @Transactional} 을 붙이지 않는다.
 * 붙이면 테스트 메소드가 끝날 때까지 영속성 컨텍스트가 살아 있어,
 * open-in-view: false 인 실제 서버에서만 나는 지연 로딩 문제를 놓친다.
 * 롤백이 없으므로 만든 것은 직접 지운다.
 */
@AutoConfigureMockMvc
class TagSerializationApiTest extends IntegrationTest {

    private static final String BASE = "/api/schedules";

    @Autowired
    MockMvc mvc;

    @Autowired
    ScheduleRepository scheduleRepository;

    @AfterEach
    void clean() {
        scheduleRepository.deleteAll();
    }

    @Test
    @DisplayName("트랜잭션이 끝난 뒤에도 태그가 응답에 실린다")
    void serializesTagsAfterTransactionEnds() throws Exception {
        String body = """
                { "title": "운동", "startAt": "2026-09-01T07:00:00", "tags": ["육체", "아침"] }""";

        long id = Long.parseLong(mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getHeader("Location")
                .replaceAll(".*/", ""));

        // 목록 — 회차로 펼쳐 나온다
        mvc.perform(get(BASE)
                        .param("from", "2026-09-01T00:00:00")
                        .param("to", "2026-09-02T00:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tags.length()").value(2));

        // 단건 — 수정 폼이 읽는 경로
        mvc.perform(get(BASE + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags.length()").value(2));

        // 후보 목록
        mvc.perform(get(BASE + "/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }
}
