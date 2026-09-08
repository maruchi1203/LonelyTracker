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
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 사용자 API
 * 가입은 지금 어디에서도 호출되지 않아, 테스트가 없으면 한 번도 실행되지 않은 채 방치된다
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
    AppProperties appProperties;

    @Test
    @DisplayName("현재 사용자는 마이그레이션이 만든 기본 사용자다")
    void me() throws Exception {
        mvc.perform(get(BASE + "/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(appProperties.user().defaultUsername()));
    }

    @Test
    @DisplayName("사용자를 만들면 201과 함께 계정이 생긴다")
    void createsUser() throws Exception {
        mvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"newbie\",\"displayName\":\"새 사용자\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("newbie"))
                .andExpect(jsonPath("$.displayName").value("새 사용자"));
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
    @DisplayName("2분 법칙은 기본으로 켜져 있고 끄면 그대로 남는다")
    void twoMinuteRuleDefaultsOnAndCanBeTurnedOff() throws Exception {
        mvc.perform(get(BASE + "/me/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.twoMinuteRule").value(true));

        mvc.perform(put(BASE + "/me/settings").contentType(MediaType.APPLICATION_JSON)
                .content("{\"twoMinuteRule\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.twoMinuteRule").value(false));

        mvc.perform(get(BASE + "/me/settings"))
                .andExpect(jsonPath("$.twoMinuteRule").value(false));
    }

    @Test
    @DisplayName("twoMinuteRule을 빼고 보내면 400을 반환한다")
    void changeSettingsRejectsMissingField() throws Exception {
        mvc.perform(put(BASE + "/me/settings").contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("없는 사용자를 조회하면 404를 반환한다")
    void findMissing() throws Exception {
        mvc.perform(get(BASE + "/99999999")).andExpect(status().isNotFound());
    }
}
