package com.lonelytracker.backend;

import com.lonelytracker.backend.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 앱이 실제로 기동되는지만 확인하는 최소 점검(스모크 테스트).
 * <p>
 * 본문은 비어 있지만 의미가 있다. 부모의 @SpringBootTest 가 앱 전체를 띄우므로
 * 설정 오타, 빈 등록 실패, DB 연결 불가, Flyway 마이그레이션 오류가 여기서 걸린다.
 * <p>
 * IntegrationTest 를 물려받아야 테스트 전용 컨테이너를 쓴다.
 * 물려받지 않으면 application.yml 의 설정을 그대로 타고 개발 DB에 붙는다.
 */
class BackendApplicationTests extends IntegrationTest {

    @Test
    @DisplayName("애플리케이션 컨텍스트가 정상적으로 로드된다")
    void contextLoads() {
    }
}
