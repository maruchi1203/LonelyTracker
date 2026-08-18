package com.lonelytracker.backend.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 기간·상태 조회는 조건이 선택적이라 JPQL에 ":param is null" 을 쓰는 대신
 * Specification(동적 쿼리)으로 처리한다. PostgreSQL은 null 비교만 있는 파라미터의
 * 타입을 추론하지 못해 "could not determine data type of parameter" 오류를 낸다.
 */
public interface ScheduleRepository extends JpaRepository<Schedule, Long>, JpaSpecificationExecutor<Schedule> {
}
