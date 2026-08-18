package com.lonelytracker.backend.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** @CreatedDate / @LastModifiedDate 를 동작시키려면 이 설정이 반드시 필요하다. */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
