package com.lonelytracker.backend.user;

import com.lonelytracker.backend.schedule.Schedule;
import com.lonelytracker.backend.schedule.ScheduleRepository;
import com.lonelytracker.backend.common.AppProperties;
import com.lonelytracker.backend.support.IntegrationTest;
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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 카테고리 목록 API. 소유자 격리와, 목록 변경이 기존 일정에 영향을 주지 않는지를 본다.
 * <p>
 * 후자가 특히 중요하다. 일정은 카테고리를 FK가 아니라 <b>이름 문자열</b>로 들고 있는데,
 * 이건 "과거 기록은 그대로 보존한다"는 설계 판단의 결과다. 코드만 봐서는 지켜지는지 알 수 없다.
 */
@AutoConfigureMockMvc
@Transactional
class UserCategoryApiTest extends IntegrationTest {

    private static final String BASE = "/api/categories";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserCategoryRepository userCategoryRepository;

    @Autowired
    ScheduleRepository scheduleRepository;

    @Autowired
    AppProperties appProperties;

    /** 요청자(기본 사용자)가 아닌 다른 사용자의 카테고리를 하나 심는다. */
    private UserCategory givenOtherUsersCategory() {
        User other = userRepository.save(User.builder()
                .username("someone-else")
                .displayName("남의 계정")
                .build());

        return userCategoryRepository.save(UserCategory.builder()
                .user(other)
                .name("남의 분류")
                .build());
    }

    // --- 소유자 격리 -------------------------------------------------------

    @Test
    @DisplayName("목록에 다른 사용자의 카테고리가 섞이지 않는다")
    void findAllExcludesOtherUsersCategory() throws Exception {
        givenOtherUsersCategory();

        assertThat(namesOf(mvc.perform(get(BASE)).andExpect(status().isOk())))
                .doesNotContain("남의 분류");
    }

    @Test
    @DisplayName("다른 사용자의 카테고리는 이름 변경이 404가 된다")
    void renameRejectsOtherUsersCategory() throws Exception {
        Long id = givenOtherUsersCategory().getId();

        mvc.perform(patch(BASE + "/" + id + "/name").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"가로채기\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("다른 사용자의 카테고리는 표시설정 변경과 삭제도 404가 된다")
    void appearanceAndDeleteRejectOtherUsersCategory() throws Exception {
        Long id = givenOtherUsersCategory().getId();

        mvc.perform(patch(BASE + "/" + id + "/appearance").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"color\":\"#ff0000\"}"))
                .andExpect(status().isNotFound());

        mvc.perform(delete(BASE + "/" + id)).andExpect(status().isNotFound());
    }

    // --- 목록 관리 ---------------------------------------------------------

    @Test
    @DisplayName("카테고리를 추가하면 201과 함께 목록에 나타난다")
    void create() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"독서\",\"color\":\"#336699\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("독서"))
                .andExpect(jsonPath("$.color").value("#336699"))
                .andExpect(jsonPath("$.archived").value(false));

        assertThat(namesOf(mvc.perform(get(BASE)))).contains("독서");
    }

    @Test
    @DisplayName("같은 사용자 안에서 이름이 중복되면 400을 반환한다")
    void createRejectsDuplicateName() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"독서\"}")).andExpect(status().isCreated());

        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"독서\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("다른 사용자와는 이름이 겹쳐도 된다")
    void nameIsUniquePerUserOnly() throws Exception {
        User other = userRepository.save(User.builder().username("other-one").build());
        userCategoryRepository.save(UserCategory.builder().user(other).name("독서").build());

        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"독서\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("쓸 수 없는 문자가 들어오면 400을 반환한다")
    void createRejectsForbiddenCharacters() throws Exception {
        // 계층이 없어져 역슬래시도 금지 문자가 됐다
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"능력\\\\개발\"}"))
                .andExpect(status().isBadRequest());
    }

    // --- 설계 검증: 목록을 바꿔도 기록은 남는다 -----------------------------

    @Test
    @DisplayName("카테고리를 목록에서 지워도 그 분류를 쓰던 일정은 그대로 남는다")
    void deletingCategoryKeepsScheduleRecords() throws Exception {
        JsonNode created = mapper.readTree(
                mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"독서\"}"))
                        .andExpect(status().isCreated())
                        .andReturn().getResponse().getContentAsString());

        Schedule schedule = scheduleRepository.save(Schedule.builder()
                .user(userRepository.findByUsername(appProperties.user().defaultUsername()).orElseThrow())
                .title("책 읽기")
                .startAt(LocalDateTime.parse("2026-10-05T20:00:00"))
                .category("독서")
                .build());

        mvc.perform(delete(BASE + "/" + created.get("id").asLong()))
                .andExpect(status().isNoContent());

        // FK가 아니라 문자열이라 제약 위반도 없고, 기록도 지워지지 않는다
        Schedule reloaded = scheduleRepository.findById(schedule.getId()).orElseThrow();
        assertThat(reloaded.getCategory())
                .as("목록에서 지웠다고 과거 일정의 분류까지 사라지면 안 된다")
                .isEqualTo("독서");
    }

    @Test
    @DisplayName("카테고리 이름을 바꿔도 이미 기록된 일정의 분류는 바뀌지 않는다")
    void renamingCategoryDoesNotTouchScheduleRecords() throws Exception {
        JsonNode created = mapper.readTree(
                mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"독서\"}"))
                        .andExpect(status().isCreated())
                        .andReturn().getResponse().getContentAsString());

        Schedule schedule = scheduleRepository.save(Schedule.builder()
                .user(userRepository.findByUsername(appProperties.user().defaultUsername()).orElseThrow())
                .title("책 읽기")
                .startAt(LocalDateTime.parse("2026-10-05T20:00:00"))
                .category("독서")
                .build());

        mvc.perform(patch(BASE + "/" + created.get("id").asLong() + "/name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"자기계발\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("자기계발"));

        Schedule reloaded = scheduleRepository.findById(schedule.getId()).orElseThrow();
        assertThat(reloaded.getCategory())
                .as("목록의 이름 변경이 과거 기록을 덮어쓰면 안 된다")
                .isEqualTo("독서");
    }

    // --- 헬퍼 -------------------------------------------------------------

    private List<String> namesOf(org.springframework.test.web.servlet.ResultActions actions) throws Exception {
        JsonNode array = mapper.readTree(actions.andReturn().getResponse().getContentAsString());
        List<String> names = new ArrayList<>();
        array.forEach(node -> names.add(node.get("name").asString()));
        return names;
    }
}
