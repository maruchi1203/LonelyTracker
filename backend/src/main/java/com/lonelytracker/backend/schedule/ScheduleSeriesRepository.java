package com.lonelytracker.backend.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ScheduleSeriesRepository extends JpaRepository<ScheduleSeries, Long> {

    /** 조회 범위에 회차를 만들어낼 수 있는 시리즈. endsOn 이 null 이면 무기한이다. */
    @Query("""
            select s from ScheduleSeries s
            where s.user.id = :userId
              and s.startsOn <= :to
              and (s.endsOn is null or s.endsOn >= :from)
            """)
    List<ScheduleSeries> findActiveIn(@Param("userId") Long userId,
                                      @Param("from") LocalDate from,
                                      @Param("to") LocalDate to);

    List<ScheduleSeries> findByUserId(Long userId);
}
