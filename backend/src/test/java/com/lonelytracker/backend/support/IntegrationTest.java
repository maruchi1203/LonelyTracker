package com.lonelytracker.backend.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 통합 테스트 공통 기반.
 * <p>
 * 실제 PostgreSQL 컨테이너를 띄우고 그 접속 정보로 datasource를 덮어쓴다.
 * H2를 쓰지 않는 이유는 Flyway 마이그레이션과 카테고리 조회가
 * generate_series, LIKE ... ESCAPE 같은 PostgreSQL 전용 문법에 의존하기 때문이다.
 * H2로 통과한 테스트는 실제 동작을 보장하지 못한다.
 * <p>
 * 컨테이너는 static 초기화 블록에서 한 번만 띄워 모든 테스트 클래스가 공유한다.
 * JUnit의 @Container를 쓰면 테스트 클래스마다 재시작되어 느리다.
 * 정리는 Testcontainers의 Ryuk 컨테이너가 JVM 종료 시 처리한다.
 * <p>
 * 스키마는 앱과 똑같이 Flyway가 만든다. 빈 DB라 V1부터 전부 실행되므로,
 * 개발 DB에서는 baseline 처리되어 한 번도 실행된 적 없는 V1__init.sql도 여기서 검증된다.
 */
@SpringBootTest
public abstract class IntegrationTest {

    // 테스트용 Docker 컨테이너
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

    static {
        POSTGRES.start();
    }

    // DynamicPropertySource의 기능
    // 1. DB 접속 정보가 담긴 application.yml 대신에 테스트용 컨테이너 정보 사용
    // 2. 
    // static 메서드인 이유
    // 1. Spring 앱 작동 전에 DB 정보를 받아와야하기 때문에
    // 2. static으로 작성해야 Test별로 Container를 생성하지 않고 같이 사용함
    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        // DB URL, Username, Password 설정
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
