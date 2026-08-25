package com.lonelytracker.backend.schedule;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ScheduleOverrideRepository extends JpaRepository<ScheduleOverride, Long> {

    Optional<ScheduleOverride> findBySeriesIdAndOnDate(Long seriesId, LocalDate onDate);

    /**
     * 조회 범위에 걸치는 override.
     * <p>
     * onDate 와 startAt 을 <b>OR</b> 로 봐야 한다. 8/31 을 9/2 로 미룬 회차는
     * onDate 로는 8월에, startAt 으로는 9월에 잡히고 <b>두 달 모두에 보여야</b> 하기 때문이다.
     */
    @Query("""
            select o from ScheduleOverride o
            where o.series.id in :seriesIds
              and (
                   (o.onDate between :fromDate and :toDate)
                or (o.startAt is not null and o.startAt between :from and :to)
              )
            """)
    List<ScheduleOverride> findInRange(@Param("seriesIds") List<Long> seriesIds,
                                       @Param("fromDate") LocalDate fromDate,
                                       @Param("toDate") LocalDate toDate,
                                       @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to);
}
