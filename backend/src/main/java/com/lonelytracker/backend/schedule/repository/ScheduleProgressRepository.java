package com.lonelytracker.backend.schedule.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import com.lonelytracker.backend.schedule.entity.ScheduleProgressEntity;

public interface ScheduleProgressRepository extends JpaRepository<ScheduleProgressEntity, Long> {

    Optional<ScheduleProgressEntity> findByScheduleIdAndOnDate(Long scheduleId, LocalDate onDate);

    /**
     * 조회 범위에 걸치는 회차 기록.
     * onDate와 startAt을 OR로 본다. 미룬 회차는 원래 달과 옮겨간 달 모두에 보여야 한다.
     */
    @Query("""
            select p from ScheduleProgressEntity p
            where p.schedule.id in :scheduleIds
              and (
                   (p.onDate between :fromDate and :toDate)
                or (p.startAt is not null and p.startAt between :from and :to)
              )
            """)
    List<ScheduleProgressEntity> findInRange(@Param("scheduleIds") List<Long> scheduleIds,
                                       @Param("fromDate") LocalDate fromDate,
                                       @Param("toDate") LocalDate toDate,
                                       @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to);

    /**
     * 일정을 지우기 전에 회차 기록을 먼저 지운다.
     * DB의 ON DELETE CASCADE를 Hibernate가 모르므로 벌크가 아닌 파생 삭제를 써야 한다.
     */
    void deleteByScheduleId(Long scheduleId);

    /**
     * 그만두기 시 앞으로의 기록을 지운다.
     * 종료일만 당기면 미뤄둔 미래 회차가 전개기에 다시 잡혀 되살아난다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from ScheduleProgressEntity p
            where p.schedule.id = :scheduleId
              and (p.onDate > :today
                or (p.startAt is not null and p.startAt > :todayEnd))
            """)
    void deleteFutureOf(@Param("scheduleId") Long scheduleId, @Param("today") LocalDate today,
                        @Param("todayEnd") LocalDateTime todayEnd);
}
