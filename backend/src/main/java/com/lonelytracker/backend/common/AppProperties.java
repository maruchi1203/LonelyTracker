package com.lonelytracker.backend.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * application.yml 의 {@code lonelytracker.*} 설정.
 * 운영 중 바뀔 수 있는 값을 코드에서 뺀다.
 */
@ConfigurationProperties(prefix = "lonelytracker")
public record AppProperties(UserDefaults user, AiSetting ai, Security security) {

    /**
     * @param encryptionKey 사용자 API 키를 DB에 암호화해 저장할 때 쓰는 마스터 키.
     *                      비어 있으면 앱은 뜨되 키를 저장·조회할 수 없다
     */
    public record Security(String encryptionKey) {
    }

    /**
     * @param connectTimeout 서버 응답 대기시간
     * @param readTimeout    응답 받는 데 걸리는 시간. 기본값은 무한 대기
     * @param maxRetries     429·5xx 나올 경우에 최대 재시도 횟수
     */
    public record AiSetting(String baseUrl, String model,
            Duration connectTimeout, Duration readTimeout, int maxRetries) {
    }

    /**
     * @param defaultUsername 인증이 붙기 전까지 현재 사용자로 취급할 계정
     */
    public record UserDefaults(String defaultUsername) {
    }
}
