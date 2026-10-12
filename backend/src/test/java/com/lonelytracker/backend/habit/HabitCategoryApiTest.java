package com.lonelytracker.backend.habit;

import com.lonelytracker.backend.habit.entity.HabitCategoryEntity;
import com.lonelytracker.backend.habit.repository.HabitCategoryRepository;
import com.lonelytracker.backend.habit.repository.HabitLogRepository;
import com.lonelytracker.backend.habit.repository.HabitRepository;
import com.lonelytracker.backend.support.IntegrationTest;
import com.lonelytracker.backend.user.entity.UserEntity;
import com.lonelytracker.backend.user.service.UserProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

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
 * 사용자가 가진 카테고리를 다루는 길을 검증한다.
 * <p>
 * {@code @Transactional} 을 붙이지 않는다. 카테고리를 지울 때 안의 습관이 함께 사라지는 일은
 * 테이블의 ON DELETE CASCADE 가 하므로, 트랜잭션이 되돌려지면 그 일이 일어났는지 볼 수 없다.
 */
@AutoConfigureMockMvc
class HabitCategoryApiTest extends IntegrationTest {

    private static final String BASE = "/api/habit-categories";
    private static final String HABITS = "/api/habits";

    /** V3 가 심어 둔 카테고리. 테스트가 만든 것만 치우고 이것들은 남겨 둔다 */
    private static final List<String> SEEDED =
            List.of("운동", "마음챙김", "부업", "예술", "학습", "인간관계");

    @Autowired
    MockMvc mvc;

    @Autowired
    HabitCategoryRepository categoryRepository;

    @Autowired
    HabitRepository habitRepository;

    @Autowired
    HabitLogRepository logRepository;

    @Autowired
    UserProvider currentUserProvider;

    @AfterEach
    void clean() {
        logRepository.deleteAll();
        habitRepository.deleteAll();
        restoreSeeded();
    }

    /**
     * 카테고리를 심어 둔 모습으로 되돌린다.
     * <p>
     * DB 를 테스트 클래스 전부가 함께 쓴다. 여기서 카테고리를 지워 놓고 가면 뒤의 것들이
     * 이름으로 카테고리를 못 찾는다. 만든 것은 치우고, 지운 것은 제자리에 다시 심는다.
     */
    /** 심어 둔 여섯의 id. display_order 차례로 온다 */
    private List<Long> seededIds() {
        return categoryRepository.findAllOf(currentUserProvider.get().getId()).stream()
                .map(HabitCategoryEntity::getId)
                .toList();
    }

    private static String orderBody(List<Long> ids) {
        return "{\"ids\":[" + ids.stream().map(String::valueOf)
                .collect(java.util.stream.Collectors.joining(",")) + "]}";
    }

    private void restoreSeeded() {
        categoryRepository.deleteAll(categoryRepository.findAll().stream()
                .filter(c -> !SEEDED.contains(c.getName()))
                .toList());

        List<HabitCategoryEntity> survivors = categoryRepository.findAll();
        List<String> left = survivors.stream()
                .map(HabitCategoryEntity::getName)
                .toList();

        // 살아남은 것의 차례도 되돌린다. 재정렬 테스트가 뒤집어 놓은 채 끝날 수 있다
        survivors.forEach(c -> c.changeDisplayOrder(SEEDED.indexOf(c.getName())));
        categoryRepository.saveAll(survivors);

        UserEntity user = currentUserProvider.get();
        categoryRepository.saveAll(SEEDED.stream()
                .filter(name -> !left.contains(name))
                .map(name -> HabitCategoryEntity.builder()
                        .user(user)
                        .name(name)
                        // 차례까지 맞춰야 "운동이 맨 앞"을 보는 테스트가 산다
                        .displayOrder(SEEDED.indexOf(name))
                        .build())
                .toList());
    }

    @Test
    @DisplayName("쓰고 있던 카테고리 여섯이 줄로 옮겨져 있다")
    void carriesTheSixSeededOnes() throws Exception {
        mvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[0].name").value("운동"))
                .andExpect(jsonPath("$[5].name").value("인간관계"));
    }

    @Test
    @DisplayName("새 카테고리는 맨 뒤에 붙는다")
    void addsToTheEnd() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"요리\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("요리"))
                .andExpect(jsonPath("$.displayOrder").value(6));

        mvc.perform(get(BASE))
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[6].name").value("요리"));
    }

    @Test
    @DisplayName("이름이 겹치면 막는다")
    void refusesADuplicateName() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"운동\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("이름만 비워도 막는다")
    void refusesABlankName() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("이름을 고쳐도 안의 습관이 그대로 붙어 있다")
    void renameKeepsTheHabits() throws Exception {
        long category = create("요리");
        long habit = addHabit(category, "설거지");

        mvc.perform(put(BASE + "/" + category).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"살림\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("살림"));

        // 습관은 id 로 카테고리를 가리킨다. 이름이 바뀌어도 가리키는 줄이 그대로다
        mvc.perform(get(HABITS))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value((int) habit))
                .andExpect(jsonPath("$[0].categoryId").value((int) category));
    }

    @Test
    @DisplayName("카테고리를 지우면 안의 습관과 그 기록까지 사라진다")
    void deleteTakesTheHabitsAndLogs() throws Exception {
        long category = create("요리");
        long habit = addHabit(category, "설거지");

        mvc.perform(put(HABITS + "/" + habit + "/logs/2026-09-11")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"done\":true}"))
                .andExpect(status().isNoContent());

        mvc.perform(delete(BASE + "/" + category)).andExpect(status().isNoContent());

        mvc.perform(get(HABITS)).andExpect(jsonPath("$.length()").value(0));
        assertThat(habitRepository.count()).isZero();
        assertThat(logRepository.count()).isZero();
    }

    @Test
    @DisplayName("마지막 카테고리는 지울 수 없다")
    void refusesToDeleteTheLastOne() throws Exception {
        List<Long> ids = categoryRepository.findAll().stream()
                .map(HabitCategoryEntity::getId)
                .toList();

        // 하나만 남을 때까지는 다 지워진다
        for (Long id : ids.subList(0, ids.size() - 1)) {
            mvc.perform(delete(BASE + "/" + id)).andExpect(status().isNoContent());
        }

        /*
         * 409 다. 보낸 것이 잘못된 게 아니라 지금 상태가 받아 주지 않는 것이고,
         * 카테고리가 둘이 되면 같은 요청이 그대로 통한다
         */
        mvc.perform(delete(BASE + "/" + ids.getLast()))
                .andExpect(status().isConflict());

        mvc.perform(get(BASE)).andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("보낸 차례대로 다시 선다")
    void reordersToTheGivenOrder() throws Exception {
        List<Long> ids = seededIds();
        List<Long> flipped = ids.reversed();

        mvc.perform(patch(BASE + "/order").contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(flipped)))
                .andExpect(status().isNoContent());

        // 조회가 display_order 로 정렬하므로 보낸 차례가 그대로 보여야 한다
        mvc.perform(get(BASE))
                .andExpect(jsonPath("$[0].name").value("인간관계"))
                .andExpect(jsonPath("$[5].name").value("운동"));
    }

    /*
     * 일부만 받으면 나머지가 어디에 설지 정할 수 없다. 남은 것을 뒤에 몰아 두면
     * 사용자가 보던 자리와 달라지므로, 아예 거절하고 화면이 전부를 보내게 한다
     */
    @Test
    @DisplayName("가진 것 전부를 보내지 않으면 거절한다")
    void refusesAPartialGroup() throws Exception {
        List<Long> some = seededIds().subList(0, 3);

        mvc.perform(patch(BASE + "/order").contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(some)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("같은 id 를 두 번 보내면 거절한다")
    void refusesADuplicate() throws Exception {
        Long first = seededIds().getFirst();

        mvc.perform(patch(BASE + "/order").contentType(MediaType.APPLICATION_JSON)
                        .content(orderBody(List.of(first, first))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("없는 카테고리는 404다")
    void missingCategoryIsNotFound() throws Exception {
        mvc.perform(delete(BASE + "/999999")).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("없는 카테고리에는 습관을 넣을 수 없다")
    void refusesAHabitInAMissingCategory() throws Exception {
        mvc.perform(post(HABITS).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"무언가\",\"categoryId\":999999}"))
                .andExpect(status().isNotFound());
    }

    private long create(String name) throws Exception {
        return idOf(mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isCreated()));
    }

    private long addHabit(long categoryId, String title) throws Exception {
        return idOf(mvc.perform(post(HABITS).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"categoryId\":" + categoryId + "}"))
                .andExpect(status().isCreated()));
    }

    /** Location 헤더 끝의 id */
    private long idOf(org.springframework.test.web.servlet.ResultActions actions) {
        return Long.parseLong(actions.andReturn().getResponse().getHeader("Location")
                .replaceAll(".*/", ""));
    }
}
