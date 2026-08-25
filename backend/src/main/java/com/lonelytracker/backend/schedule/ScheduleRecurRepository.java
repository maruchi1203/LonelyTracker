package com.lonelytracker.backend.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ScheduleRecurRepository extends JpaRepository<ScheduleRecur, Long> {

    /** 조회 대상 일정들 중 반복 규칙이 붙어 있는 것. 없으면 1회성이다. */
    @Query("select r from ScheduleRecur r where r.scheduleId in :scheduleIds")
    List<ScheduleRecur> findByScheduleIds(@Param("scheduleIds") List<Long> scheduleIds);
}
