package com.lonelytracker.backend.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 제공자 주소를 한 모양으로 맞추는 규칙.
 * 같은 서버를 가리키는 표기가 두 줄로 갈리면 키를 바꿨는데 옛 줄로 부르는 일이 생긴다.
 * Spring 컨텍스트를 띄우지 않으므로 Docker 없이 돈다.
 */
class AiBaseUrlsTest {

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @DisplayName("같은 서버를 가리키는 표기는 한 모양이 된다")
    @CsvSource(delimiter = '|', value = {
            "'  https://api.openai.com/v1/  '                           | https://api.openai.com/v1",
            "HTTPS://API.OPENAI.COM/v1                                  | https://api.openai.com/v1",
            "https://api.openai.com:443/v1                              | https://api.openai.com/v1",
            "http://localhost:80/v1                                     | http://localhost/v1",
            "http://localhost:11434/v1                                  | http://localhost:11434/v1",
            "https://x.test/V1                                          | https://x.test/V1",
            "https://x.test/v1//                                        | https://x.test/v1",
            "https://generativelanguage.googleapis.com/v1beta/openai/   | https://generativelanguage.googleapis.com/v1beta/openai",
            "https://api.openai.com                                     | https://api.openai.com",
            "https://x.test/v1/%EB%AC%B8%EC%84%9C                       | https://x.test/v1/%EB%AC%B8%EC%84%9C",
    })
    void normalizes(String raw, String expected) {
        assertThat(AiBaseUrls.normalize(raw)).isEqualTo(expected);
    }

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @DisplayName("주소로 쓸 수 없는 값은 이유와 함께 거절한다")
    @CsvSource(delimiter = '|', value = {
            "api.openai.com/v1              | http 또는 https",
            "ftp://x.test/v1                | http 또는 https",
            "https:///v1                    | 호스트가 없습니다",
            "http://my_server:11434/v1      | 밑줄",
            "https://x.test/v1?key=abc      | ? 나 #",
            "https://x.test/v1#top          | ? 나 #",
            "https://user:pass@x.test/v1    | 계정 정보",
            "'\"https://x.test/v1\"'        | 형식이 올바르지 않습니다",
            "https://x.test/v 1             | 형식이 올바르지 않습니다",
            "''                             | http 또는 https",
    })
    void rejects(String raw, String reason) {
        assertThatThrownBy(() -> AiBaseUrls.normalize(raw))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(reason);
    }
}
