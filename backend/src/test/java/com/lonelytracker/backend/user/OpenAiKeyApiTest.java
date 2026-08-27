package com.lonelytracker.backend.user;

import com.lonelytracker.backend.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 사용자별 OpenAI API 키.
 * <p>
 * 키는 <b>서버 설정이 아니라 사용자가 갖는다</b> — 서버가 모두의 사용료를 대신 낼
 * 이유가 없고 사용량도 쓰는 사람에게 귀속되어야 한다. DB 접속 정보는 반대다.
 * <p>
 * API 키는 비밀번호와 같은 등급이라 <b>암호화해 저장하고 어떤 응답에도 원본을 싣지 않는다</b>.
 */
@AutoConfigureMockMvc
@Transactional
class OpenAiKeyApiTest extends IntegrationTest {

    private static final String KEY_PATH = "/api/users/me/openai-key";
    private static final String SAMPLE_KEY = "sk-proj-verysecretvalue1234";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    @DisplayName("등록하면 마스킹된 값만 돌려준다")
    void registerReturnsMaskedOnly() throws Exception {
        mvc.perform(put(KEY_PATH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apiKey\":\"" + SAMPLE_KEY + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registered").value(true))
                .andExpect(jsonPath("$.masked").value("****1234"));
    }

    @Test
    @DisplayName("조회해도 키 원본은 나오지 않는다")
    void statusNeverExposesKey() throws Exception {
        mvc.perform(put(KEY_PATH).contentType(MediaType.APPLICATION_JSON)
                .content("{\"apiKey\":\"" + SAMPLE_KEY + "\"}")).andExpect(status().isOk());

        String body = mvc.perform(get(KEY_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registered").value(true))
                .andReturn().getResponse().getContentAsString();

        // 전체를 보여줄 이유가 없다. 보여주는 순간 화면 캡처·로그로 샐 자리가 는다
        assertThat(body).doesNotContain(SAMPLE_KEY);
    }

    @Test
    @DisplayName("사용자 조회 응답에도 키가 섞이지 않는다")
    void userResponseNeverExposesKey() throws Exception {
        mvc.perform(put(KEY_PATH).contentType(MediaType.APPLICATION_JSON)
                .content("{\"apiKey\":\"" + SAMPLE_KEY + "\"}")).andExpect(status().isOk());

        String body = mvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain(SAMPLE_KEY);
    }

    @Test
    @DisplayName("DB에는 평문이 아니라 암호문이 저장된다")
    void storedValueIsEncrypted() throws Exception {
        mvc.perform(put(KEY_PATH).contentType(MediaType.APPLICATION_JSON)
                .content("{\"apiKey\":\"" + SAMPLE_KEY + "\"}")).andExpect(status().isOk());

        String stored = jdbc.queryForObject(
                "select openai_api_key from app_user where username = 'default'", String.class);

        // 암호화한다고 해놓고 평문이 남으면 아무 의미가 없다.
        // DB 백업이나 덤프가 새는 순간 키가 그대로 노출된다
        assertThat(stored).isNotNull();
        assertThat(stored).doesNotContain(SAMPLE_KEY);
        assertThat(stored).isNotEqualTo(SAMPLE_KEY);
    }

    @Test
    @DisplayName("키를 바꾸면 저장값도 바뀌고 이전 값은 남지 않는다")
    void replacingKeyUpdatesStoredValue() throws Exception {
        mvc.perform(put(KEY_PATH).contentType(MediaType.APPLICATION_JSON)
                .content("{\"apiKey\":\"" + SAMPLE_KEY + "\"}")).andExpect(status().isOk());
        String first = jdbc.queryForObject(
                "select openai_api_key from app_user where username = 'default'", String.class);

        mvc.perform(put(KEY_PATH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apiKey\":\"sk-proj-anothervalue5678\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.masked").value("****5678"));

        String second = jdbc.queryForObject(
                "select openai_api_key from app_user where username = 'default'", String.class);

        assertThat(second).isNotEqualTo(first);
    }

    @Test
    @DisplayName("빈 값을 주면 등록이 해제된다")
    void blankValueClearsKey() throws Exception {
        mvc.perform(put(KEY_PATH).contentType(MediaType.APPLICATION_JSON)
                .content("{\"apiKey\":\"" + SAMPLE_KEY + "\"}")).andExpect(status().isOk());

        mvc.perform(put(KEY_PATH).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"apiKey\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registered").value(false))
                .andExpect(jsonPath("$.masked").doesNotExist());
    }

    @Test
    @DisplayName("등록 전에는 registered가 false다")
    void notRegisteredInitially() throws Exception {
        mvc.perform(get(KEY_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registered").value(false));
    }
}
