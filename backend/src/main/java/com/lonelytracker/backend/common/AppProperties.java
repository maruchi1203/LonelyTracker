package com.lonelytracker.backend.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

/**
 * application.yml 의 {@code lonelytracker.*} 설정.
 * <p>
 * 운영 중 바뀔 수 있는 값을 코드에서 뺀다. 길이 제한처럼 DB 스키마와 짝을 이뤄야 하는 값은
 * 여기가 아니라 {@link FieldLengths} 에 둔다. 그쪽은 마음대로 바꾸면 안 되는 값이기 때문이다.
 */
@ConfigurationProperties(prefix = "lonelytracker")
public record AppProperties(UserDefaults user, Ai ai) {

    /**
     * LLM 연동 설정.
     *
     * @param apiKey         비어 있으면 <b>앱은 뜨되 파싱 기능만 막힌다</b>.
     *                       AI 없이도 일정 CRUD 는 되므로 기동을 막을 이유가 없다
     * @param connectTimeout 서버에 붙는 데 걸리는 시간
     * @param readTimeout    응답을 다 받는 데 걸리는 시간. 기본값이 무한 대기라
     *                       걸지 않으면 API 가 멈췄을 때 우리 스레드가 계속 묶인다
     * @param maxRetries     429·5xx 만 재시도한다. 4xx 는 몇 번을 보내도 같은 결과다
     */
    public record Ai(String apiKey, String baseUrl, String model,
                     Duration connectTimeout, Duration readTimeout, int maxRetries) {

        public boolean configured() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    /**
     * @param defaultUsername       인증이 붙기 전까지 현재 사용자로 취급할 계정.
     *                              V4 마이그레이션이 만들어 둔 이름과 같아야 한다.
     * @param recommendedCategories 가입 시 미리 넣어주는 카테고리.
     *                              빈 사이드바로 시작하면 무엇부터 적어야 할지 막막하므로 출발점을 준다.
     */
    public record UserDefaults(String defaultUsername, List<String> recommendedCategories) {
    }
}
