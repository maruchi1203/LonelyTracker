package com.lonelytracker.backend.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * application.yml 의 {@code lonelytracker.*} 설정.
 * <p>
 * 운영 중 바뀔 수 있는 값을 코드에서 뺀다. 길이 제한처럼 DB 스키마와 짝을 이뤄야 하는 값은
 * 여기가 아니라 {@link FieldLengths} 에 둔다. 그쪽은 마음대로 바꾸면 안 되는 값이기 때문이다.
 */
@ConfigurationProperties(prefix = "lonelytracker")
public record AppProperties(UserDefaults user) {

    /**
     * @param defaultUsername       인증이 붙기 전까지 현재 사용자로 취급할 계정.
     *                              V4 마이그레이션이 만들어 둔 이름과 같아야 한다.
     * @param recommendedCategories 가입 시 미리 넣어주는 카테고리.
     *                              빈 사이드바로 시작하면 무엇부터 적어야 할지 막막하므로 출발점을 준다.
     */
    public record UserDefaults(String defaultUsername, List<String> recommendedCategories) {
    }
}
