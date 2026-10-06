package com.lonelytracker.backend.common;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * "오늘"을 밖에서 넣어 주기 위한 시계.
 * <p>
 * {@code LocalDate.now()} 를 직접 부르면 테스트가 오늘을 고정할 수 없어,
 * 날짜를 적어 둔 테스트가 그 날이 지나면 깨진다. 실제로 그렇게 한 번 깨졌다.
 * <p>
 * 테스트는 이 빈을 {@code Clock.fixed(...)} 로 갈아끼워 시간을 멈춘다.
 * <p>
 * 묶지 않은 자리가 둘 있다. 엔티티의 생성·수정·완료 시각은 감사 기록에 가깝고,
 * {@code ScheduleService.changeCompletion} 의 완료 시각은 그 값으로 "같이 끝난 자손"과
 * "먼저 끝낸 자손"을 가르기 때문에 멈추면 구실을 잃는다.
 */
@Configuration
public class ClockConfig {

    @Bean
    @ConditionalOnMissingBean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
