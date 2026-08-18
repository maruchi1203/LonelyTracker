package com.lonelytracker.backend.schedule;

import com.lonelytracker.backend.category.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

/**
 * 기간·상태·카테고리 조회는 조건이 선택적이라 Specification(동적 쿼리)으로 처리한다.
 * JPQL에 ":param is null" 을 쓰면 PostgreSQL이 파라미터 타입을 추론하지 못해
 * "could not determine data type of parameter" 오류가 난다.
 */
public interface ScheduleRepository extends JpaRepository<Schedule, Long>, JpaSpecificationExecutor<Schedule> {

    /**
     * 카테고리 삭제 시 호출. 일정은 지우지 않고 다른 분류로 옮긴다.
     * {@code newCategory} 가 null이면 미분류가 된다(최상위 카테고리를 지운 경우).
     * <p>
     * {@code s.category.id} 는 FK 컬럼을 그대로 쓰므로 조인이 생기지 않는다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Schedule s set s.category = :newCategory where s.category.id in :categoryIds")
    int moveCategory(@Param("categoryIds") Collection<Long> categoryIds,
                     @Param("newCategory") Category newCategory);
}
