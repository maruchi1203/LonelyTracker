package com.lonelytracker.backend.schedule.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.lonelytracker.backend.schedule.entity.ScheduleEntity;

public interface ScheduleRepository extends JpaRepository<ScheduleEntity, Long> {
  /**
   * 조회 범위에 회차를 낼 수 있는 일정 후보를 모은다.
   * 반복 일정, 범위 안의 1회성 일정, 미뤄서 범위로 들어온 회차를 가진 일정을 OR로 묶는다.
   * 정확한 날짜 판정은 전개기가 하고 여기서는 후보만 좁힌다.
   *
   * @param from 조회 시작 시각
   * @param to   조회 끝 시각
   */
  @Query("""
      select s from ScheduleEntity s
      where s.user.id = :userId
        and s.startAt <= :to
        and (
             exists (select 1 from ScheduleRecurEntity r
                      where r.scheduleId = s.id
                        and (r.endsOn is null or r.endsOn >= :fromDate))
          or s.startAt >= :from
          or exists (select 1 from ScheduleProgressEntity p
                      where p.schedule.id = s.id
                        and ((p.onDate between :fromDate and :toDate)
                          or (p.startAt is not null and p.startAt between :from and :to)))
        )
      """)
  List<ScheduleEntity> findCandidates(@Param("userId") Long userId,
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to,
      @Param("fromDate") LocalDate fromDate,
      @Param("toDate") LocalDate toDate);
}
