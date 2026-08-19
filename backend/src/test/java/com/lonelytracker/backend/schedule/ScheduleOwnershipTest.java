package com.lonelytracker.backend.schedule;

import com.lonelytracker.backend.support.IntegrationTest;
import com.lonelytracker.backend.user.User;
import com.lonelytracker.backend.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 일정이 소유자 단위로 격리되는지 검증한다.
 * <p>
 * 지금은 사용자가 하나뿐이라 남의 일정이 섞여도 눈에 띄지 않는다.
 * 조회 조건에서 소유자 필터가 빠지는 실수를 잡으려면 다른 사용자의 데이터를 직접 만들어
 * 확인하는 수밖에 없다.
 * <p>
 * 인증이 아직 없어 요청자는 항상 기본 사용자다. 그래서 "남의 것"은 리포지토리로 직접 심는다.
 */
@AutoConfigureMockMvc
@Transactional
class ScheduleOwnershipTest extends IntegrationTest {

    private static final String BASE = "/api/schedules";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    UserRepository userRepository;

    @Autowired
    ScheduleRepository scheduleRepository;

    /** 요청자(기본 사용자)가 아닌 다른 사용자의 일정을 하나 심는다. */
    private Schedule givenOtherUsersSchedule() {
        User other = userRepository.save(User.builder()
                .username("someone-else")
                .displayName("남의 계정")
                .build());

        return scheduleRepository.save(Schedule.builder()
                .user(other)
                .title("남의 일정")
                .startAt(LocalDateTime.parse("2026-10-01T10:00:00"))
                .category("능력")
                .build());
    }

    @Test
    @DisplayName("목록에 다른 사용자의 일정이 섞이지 않는다")
    void searchExcludesOtherUsersSchedule() throws Exception {
        givenOtherUsersSchedule();

        JsonNode found = mapper.readTree(
                mvc.perform(get(BASE))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("분류로 걸러도 다른 사용자의 일정은 나오지 않는다")
    void searchByCategoryExcludesOtherUsersSchedule() throws Exception {
        givenOtherUsersSchedule();

        JsonNode found = mapper.readTree(
                mvc.perform(get(BASE).param("category", "능력"))
                        .andExpect(status().isOk())
                        .andReturn().getResponse().getContentAsString());

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("다른 사용자의 일정은 단건 조회에서 404가 된다")
    void findByIdRejectsOtherUsersSchedule() throws Exception {
        Long id = givenOtherUsersSchedule().getId();

        mvc.perform(get(BASE + "/" + id)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("다른 사용자의 일정은 수정·상태변경·삭제도 404가 된다")
    void mutationsRejectOtherUsersSchedule() throws Exception {
        Long id = givenOtherUsersSchedule().getId();

        String body = """
                {"title":"덮어쓰기 시도","startAt":"2026-10-01T12:00:00"}
                """;

        mvc.perform(put(BASE + "/" + id).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isNotFound());

        mvc.perform(patch(BASE + "/" + id + "/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"DONE\"}"))
                .andExpect(status().isNotFound());

        mvc.perform(delete(BASE + "/" + id)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("생성한 일정은 요청자 소유로 저장된다")
    void createAssignsCurrentUser() throws Exception {
        String body = """
                {"title":"내 일정","startAt":"2026-10-02T09:00:00"}
                """;

        JsonNode created = mapper.readTree(
                mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(body))
                        .andExpect(status().isCreated())
                        .andReturn().getResponse().getContentAsString());

        Schedule saved = scheduleRepository.findById(created.get("id").asLong()).orElseThrow();

        assertThat(saved.getUser().getUsername())
                .as("생성된 일정의 소유자가 요청자와 다르다")
                .isEqualTo("default");
    }
}
