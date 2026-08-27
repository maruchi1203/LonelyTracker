package com.lonelytracker.backend.ai;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 자연어를 일정 초안으로 바꾼다. <b>이 프로젝트의 유일한 LLM 경계다.</b>
 * <p>
 * 인터페이스를 두는 진짜 이유는 제공자 교체가 아니라 <b>테스트</b>다.
 * 이 경계가 있으면 API 를 한 번도 부르지 않고 검증 로직·실패 처리·화면 흐름을
 * 전부 테스트할 수 있다. 빠르고, 비용이 0이고, 네트워크에 안 묶인다.
 */
public interface ScheduleParser {

    /**
     * @param now        기준 시각. <b>인자로 받는 것이 요점이다.</b> 안에서
     *                   {@code LocalDateTime.now()} 를 부르면 "내일 3시" 가 제대로
     *                   해석되는지 테스트할 방법이 없다
     * @param categories 사용자의 카테고리 목록. 이 안에서만 고르게 한다.
     *                   주지 않으면 모델이 없는 분류를 만들어 낸다
     */
    ParsedSchedule parse(String text, LocalDateTime now, List<String> categories);
}
