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
public record AppProperties(UserDefaults user, Ai ai, Security security) {

    /**
     * @param encryptionKey 사용자 API 키를 DB 에 암호화해 저장할 때 쓰는 마스터 키.
     *                      <b>이건 서버의 것이라 서버 설정에 두는 게 맞다.</b>
     *                      비어 있으면 앱은 뜨되 키를 저장·조회할 수 없다
     */
    public record Security(String encryptionKey) {
    }


    /**
     * LLM 연동 설정.
     *
     * API 키는 여기 없다. <b>사용자별로 갖는다</b> - 서버가 모두의 사용료를 대신 낼
     * 이유가 없고 사용량도 쓰는 사람에게 귀속되어야 한다. 여기 남는 것은
     * 사용자와 무관한 인프라 설정뿐이다.
     *
     * @param connectTimeout 서버에 붙는 데 걸리는 시간
     * @param readTimeout    응답을 다 받는 데 걸리는 시간. 기본값이 무한 대기라
     *                       걸지 않으면 API 가 멈췄을 때 우리 스레드가 계속 묶인다
     * @param maxRetries     429·5xx 만 재시도한다. 4xx 는 몇 번을 보내도 같은 결과다
     */
    public record Ai(String baseUrl, String model,
                     Duration connectTimeout, Duration readTimeout, int maxRetries) {
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
