package com.lonelytracker.backend.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 기간·상태·카테고리 조회는 조건이 선택적이라 Specification(동적 쿼리)으로 처리한다.
 * JPQL에 ":param is null" 을 쓰면 PostgreSQL이 파라미터 타입을 추론하지 못해
 * "could not determine data type of parameter" 오류가 난다.
 */
public interface ScheduleRepository extends JpaRepository<Schedule, Long>, JpaSpecificationExecutor<Schedule> {

    /** 카테고리 삭제 시 호출. 일정은 지우지 않고 미분류로 남긴다. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Schedule s set s.category = null where s.category.id = :categoryId")
    int clearCategory(@Param("categoryId") Long categoryId);
}
