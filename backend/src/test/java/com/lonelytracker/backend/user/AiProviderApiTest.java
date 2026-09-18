package com.lonelytracker.backend.user;

import com.jayway.jsonpath.JsonPath;
import com.lonelytracker.backend.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AI 제공자별 제공자 설정.
 * <p>
 * 제공자를 바꿔도 앞서 넣은 키가 남아야 한다. 키 하나를 갈아 끼우는 방식은
 * 주소와 키가 어긋나 Gemini 키가 OpenAI 로 날아가는 사고를 낸다.
 * <p>
 * API 키는 비밀번호와 같은 등급이라 <b>암호화해 저장하고 어떤 응답에도 원본을 싣지 않는다</b>.
 */
@AutoConfigureMockMvc
@Transactional
class AiProviderApiTest extends IntegrationTest {

    private static final String PATH = "/api/users/me/ai-providers";
    private static final String OPENAI = "https://api.openai.com/v1";
    private static final String GEMINI = "https://generativelanguage.googleapis.com/v1beta/openai";
    private static final String SAMPLE_KEY = "sk-proj-verysecretvalue1234";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    @DisplayName("등록 전에는 목록이 비어 있고 서버 설정도 없다")
    void emptyInitially() throws Exception {
        mvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providers").isEmpty())
                .andExpect(jsonPath("$.serverConfigured").value(false));
    }

    @Test
    @DisplayName("등록하면 마스킹된 값만 돌려주고 바로 쓰는 것으로 고른다")
    void registerReturnsMaskedAndActivates() throws Exception {
        save(OPENAI, "gpt-5.6-luna", SAMPLE_KEY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.masked").value("****1234"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    @DisplayName("목록에도 키 원본은 나오지 않는다")
    void listNeverExposesKey() throws Exception {
        save(OPENAI, "gpt-5.6-luna", SAMPLE_KEY).andExpect(status().isOk());

        String body = mvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // 보여주는 순간 화면 캡처·로그로 샐 자리가 는다
        assertThat(body).doesNotContain(SAMPLE_KEY);
    }

    @Test
    @DisplayName("사용자 조회 응답에도 키가 섞이지 않는다")
    void userResponseNeverExposesKey() throws Exception {
        save(OPENAI, "gpt-5.6-luna", SAMPLE_KEY).andExpect(status().isOk());

        String body = mvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain(SAMPLE_KEY);
    }

    @Test
    @DisplayName("DB에는 평문이 아니라 암호문이 저장된다")
    void storedValueIsEncrypted() throws Exception {
        save(OPENAI, "gpt-5.6-luna", SAMPLE_KEY).andExpect(status().isOk());

        String stored = jdbc.queryForObject(
                "select api_key from ai_provider where base_url = ?", String.class, OPENAI);

        // DB 백업이나 덤프가 새는 순간 평문 키는 그대로 노출된다
        assertThat(stored).isNotNull();
        assertThat(stored).doesNotContain(SAMPLE_KEY);
    }

    @Test
    @DisplayName("제공자를 바꿔도 앞서 넣은 키가 남는다")
    void switchingProviderKeepsPreviousKey() throws Exception {
        save(OPENAI, "gpt-5.6-luna", SAMPLE_KEY).andExpect(status().isOk());
        save(GEMINI, "gemini-2.5-flash", "AIza-gemini-key-5678").andExpect(status().isOk());

        mvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providers.length()").value(2))
                .andExpect(jsonPath("$.providers[0].baseUrl").value(OPENAI))
                .andExpect(jsonPath("$.providers[0].active").value(false))
                .andExpect(jsonPath("$.providers[1].baseUrl").value(GEMINI))
                .andExpect(jsonPath("$.providers[1].active").value(true));
    }

    @Test
    @DisplayName("같은 주소로 다시 저장하면 줄이 늘지 않고 키가 바뀐다")
    void sameBaseUrlReplacesKey() throws Exception {
        save(OPENAI, "gpt-5.6-luna", SAMPLE_KEY).andExpect(status().isOk());
        save(OPENAI, "gpt-5.6-luna", "sk-proj-anothervalue5678")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.masked").value("****5678"));

        mvc.perform(get(PATH))
                .andExpect(jsonPath("$.providers.length()").value(1));
    }

    @Test
    @DisplayName("표기만 다른 같은 주소는 한 줄로 합쳐진다")
    void differentlyWrittenSameUrlIsOneRow() throws Exception {
        save(OPENAI + "/", "gpt-5.6-luna", SAMPLE_KEY).andExpect(status().isOk());
        save("HTTPS://API.OPENAI.COM:443/v1", "gpt-5.6-luna", "sk-proj-anothervalue5678")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseUrl").value(OPENAI));

        mvc.perform(get(PATH))
                .andExpect(jsonPath("$.providers.length()").value(1));
    }

    @Test
    @DisplayName("주소로 쓸 수 없는 값은 400 과 이유를 준다")
    void invalidBaseUrlIsRejectedWithReason() throws Exception {
        save("api.openai.com/v1", "gpt-5.6-luna", SAMPLE_KEY)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("http 또는 https 로 시작하는 주소를 넣어 주세요"));
    }

    @Test
    @DisplayName("이미 있는 주소는 키를 비워도 모델만 바뀐다")
    void blankKeyOnExistingChangesModelOnly() throws Exception {
        save(OPENAI, "gpt-5.6-luna", SAMPLE_KEY).andExpect(status().isOk());

        save(OPENAI, "gpt-5.6-terra", "")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.model").value("gpt-5.6-terra"))
                .andExpect(jsonPath("$.masked").value("****1234"));
    }

    @Test
    @DisplayName("새 주소에 키가 없으면 400")
    void newBaseUrlRequiresKey() throws Exception {
        save(GEMINI, "gemini-2.5-flash", "")
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("모델을 비우면 400")
    void modelIsRequired() throws Exception {
        save(GEMINI, "", SAMPLE_KEY)
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("다른 줄로 전환하면 그 줄만 사용 중이 된다")
    void activateSwitchesActive() throws Exception {
        long openAi = idOf(save(OPENAI, "gpt-5.6-luna", SAMPLE_KEY));
        save(GEMINI, "gemini-2.5-flash", "AIza-gemini-key-5678").andExpect(status().isOk());

        mvc.perform(put(PATH + "/" + openAi + "/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        mvc.perform(get(PATH))
                .andExpect(jsonPath("$.providers[0].active").value(true))
                .andExpect(jsonPath("$.providers[1].active").value(false));
    }

    @Test
    @DisplayName("쓰던 것을 지우면 고른 것이 없어진다")
    void deletingActiveClearsSelection() throws Exception {
        long id = idOf(save(OPENAI, "gpt-5.6-luna", SAMPLE_KEY));

        mvc.perform(delete(PATH + "/" + id)).andExpect(status().isNoContent());

        mvc.perform(get(PATH))
                .andExpect(jsonPath("$.providers").isEmpty());
        Long active = jdbc.queryForObject(
                "select active_ai_provider_id from app_user where username = 'default'", Long.class);
        assertThat(active).isNull();
    }

    @Test
    @DisplayName("남의 제공자 설정은 고르거나 지울 수 없다")
    void cannotTouchOthersProvider() throws Exception {
        mvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"other\",\"displayName\":\"other\"}"))
                .andExpect(status().isCreated());
        Long otherUser = jdbc.queryForObject(
                "select id from app_user where username = 'other'", Long.class);
        Long othersProvider = jdbc.queryForObject(
                "insert into ai_provider (user_id, base_url, model, api_key, created_at, updated_at) "
                        + "values (?, ?, 'm', 'x', now(), now()) returning id",
                Long.class, otherUser, OPENAI);

        // 있다고 알려주지 않는다
        mvc.perform(put(PATH + "/" + othersProvider + "/active")).andExpect(status().isNotFound());
        mvc.perform(delete(PATH + "/" + othersProvider)).andExpect(status().isNotFound());
    }

    // --- 헬퍼 -------------------------------------------------------------

    private ResultActions save(String baseUrl, String model, String apiKey) throws Exception {
        return mvc.perform(put(PATH).contentType(MediaType.APPLICATION_JSON)
                .content("{\"baseUrl\":\"" + baseUrl + "\",\"model\":\"" + model
                        + "\",\"apiKey\":\"" + apiKey + "\"}"));
    }

    private long idOf(ResultActions result) throws Exception {
        String body = result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }
}
