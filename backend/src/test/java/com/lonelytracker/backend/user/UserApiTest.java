package com.lonelytracker.backend.user;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.lonelytracker.backend.user.entity.UserCategoryEntity;
import com.lonelytracker.backend.user.repository.UserCategoryRepository;

/**
 * 사용자 API. 가입 시 추천 카테고리가 함께 만들어지는지가 핵심이다.
 * <p>
 * 이 시딩은 지금 어디에서도 호출되지 않아, 테스트가 없으면 한 번도 실행되지 않은 채
 * 방치된다. 나중에 회원가입을 붙이는 순간 처음 돌아가게 되는데 그때 깨져 있으면 곤란하다.
 */
@AutoConfigureMockMvc
@Transactional
class UserApiTest extends IntegrationTest {

    private static final String BASE = "/api/users";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper mapper;

    @Autowired
    UserCategoryRepository userCategoryRepository;

    @Autowired
    AppProperties appProperties;

    @Test
    @DisplayName("현재 사용자는 마이그레이션이 만든 기본 사용자다")
    void me() throws Exception {
        mvc.perform(get(BASE + "/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(appProperties.user().defaultUsername()));
    }

    @Test
    @DisplayName("사용자를 만들면 추천 카테고리가 함께 생성된다")
    void createSeedsRecommendedCategories() throws Exception {
        JsonNode created = mapper.readTree(
                mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"newbie\",\"displayName\":\"새 사용자\"}"))
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.username").value("newbie"))
                        .andReturn().getResponse().getContentAsString());

        var seeded = userCategoryRepository
                .findByUserIdOrderByDisplayOrderAscNameAsc(created.get("id").asLong())
                .stream()
                .map(UserCategoryEntity::getName)
                .toList();

        assertThat(seeded)
                .as("가입 직후 사이드바가 비어 있으면 무엇부터 적어야 할지 막막하다")
                .containsExactlyElementsOf(appProperties.user().recommendedCategories());
    }

    @Test
    @DisplayName("displayName을 비우면 username을 대신 쓴다")
    void createFallsBackToUsernameAsDisplayName() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"nameless\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.displayName").value("nameless"));
    }

    @Test
    @DisplayName("이미 있는 username이면 400을 반환한다")
    void createRejectsDuplicateUsername() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + appProperties.user().defaultUsername() + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("OAuth가 주는 이메일 형태의 username도 그대로 받는다")
    void createAcceptsEmailAsUsername() throws Exception {
        // 로그인을 OAuth로만 받기로 해서 형식을 우리가 정할 수 없다.
        // 영문·숫자만 허용하던 시절에는 @ 와 . 때문에 이메일이 전부 거부됐다.
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"someone@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("someone@example.com"));
    }

    @Test
    @DisplayName("없는 사용자를 조회하면 404를 반환한다")
    void findMissing() throws Exception {
        mvc.perform(get(BASE + "/99999999")).andExpect(status().isNotFound());
    }
}
