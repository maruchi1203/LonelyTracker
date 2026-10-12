package com.lonelytracker.backend.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * 시간을 멈춘다. 날짜를 적어 둔 테스트가 그 날이 지나도 깨지지 않게 한다.
 * <p>
 * 쓰려면 테스트 클래스에 {@code @Import(FixedClockConfig.class)} 를 붙이고,
 * 날짜는 {@link #TODAY} 를 기준으로 적는다. 운영 쪽 시계 빈이
 * {@code @ConditionalOnMissingBean} 이라 이 빈이 그 자리를 대신한다.
 * <p>
 * 모든 테스트에 걸지 않는 까닭은, 이미 {@code LocalDate.now()} 로 짓는 테스트들은
 * 그 자체로 날짜에 매이지 않아 멈춘 시계와 오히려 어긋나기 때문이다.
 */
@TestConfiguration
public class FixedClockConfig {

    /** 멈춰 둔 "오늘" */
    public static final LocalDate TODAY = LocalDate.of(2026, 10, 1);

    @Bean
    Clock clock() {
        ZoneId zone = ZoneId.systemDefault();
        return Clock.fixed(TODAY.atStartOfDay(zone).toInstant(), zone);
    }
}
