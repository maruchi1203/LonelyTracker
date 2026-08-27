package com.lonelytracker.backend.ai;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 파싱 한 번에 필요한 것 전부.
 * <p>
 * 인자를 늘어놓지 않고 묶는 이유는 <b>apiKey 가 비밀 값이라는 게 시그니처에서
 * 보여야</b> 하고, 나중에 사용자별 모델 선택 같은 것이 붙어도 인터페이스가
 * 흔들리지 않기 때문이다.
 *
 * @param now        기준 시각. <b>인자로 받는 것이 요점이다.</b> 안에서 now() 를 부르면
 *                   "내일 3시" 가 제대로 해석되는지 테스트할 방법이 없다
 * @param categories 사용자의 카테고리 목록. 이 안에서만 고르게 한다
 * @param apiKey     <b>사용자의</b> OpenAI API 키. 서버 설정이 아니다
 */
public record ParseCommand(
        String text,
        LocalDateTime now,
        List<String> categories,
        String apiKey
) {
}
