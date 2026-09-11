package com.lonelytracker.backend.habit.repository;

import com.lonelytracker.backend.habit.entity.HabitLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface HabitLogRepository extends JpaRepository<HabitLogEntity, Long> {

    Optional<HabitLogEntity> findByHabitIdAndOnDate(Long habitId, LocalDate onDate);

    /**
     * 그 구간에 남은 기록 전부.
     * 일지가 한 번에 며칠을 그리므로 날마다 묻지 않는다.
     */
    @Query("""
            select l from HabitLogEntity l
            where l.habit.id in :habitIds
              and l.onDate between :from and :to
            """)
    List<HabitLogEntity> findInRange(@Param("habitIds") Collection<Long> habitIds,
            @Param("from") LocalDate from, @Param("to") LocalDate to);
}
