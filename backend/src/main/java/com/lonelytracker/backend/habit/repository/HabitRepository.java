package com.lonelytracker.backend.habit.repository;

import com.lonelytracker.backend.habit.entity.HabitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HabitRepository extends JpaRepository<HabitEntity, Long> {

    /**
     * 그 사람의 습관 전부. 그만둔 것도 함께 온다.
     * 화면이 카테고리로 묶고 걸러낸다. 정렬은 서비스가 맡는다.
     */
    @Query("select h from HabitEntity h where h.user.id = :userId")
    List<HabitEntity> findAllOf(@Param("userId") Long userId);

    /**
     * 한 카테고리에 선 습관 전부. 차례를 다시 매길 때 쓴다.
     * 그만둔 습관도 함께 온다. 화면이 보는 무리와 같아야 재정렬이 늘 거절되지 않는다.
     */
    @Query("select h from HabitEntity h"
            + " where h.user.id = :userId and h.category.id = :categoryId")
    List<HabitEntity> findInCategory(@Param("userId") Long userId,
            @Param("categoryId") Long categoryId);

    /** 남의 습관을 건드리지 못하게 소유자까지 함께 본다 */
    @Query("select h from HabitEntity h where h.id = :id and h.user.id = :userId")
    Optional<HabitEntity> findOwned(@Param("id") Long id, @Param("userId") Long userId);
}
