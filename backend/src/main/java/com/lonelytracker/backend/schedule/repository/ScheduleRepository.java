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
     * 조회 범위에 회차를 낼 수 있는 일정 후보.
     * <p>
     * 세 갈래를 OR 로 모은다.
     * <ol>
     *   <li>반복 일정 — 시작이 범위 끝보다 앞이고, 종료일이 없거나 범위 시작보다 뒤</li>
     *   <li>단일 일정 — 자기 날짜가 범위 안</li>
     *   <li>미뤄서 범위로 들어온 회차를 가진 일정 — 자기 날짜는 범위 밖일 수 있다</li>
     * </ol>
     * 정확한 날짜 판정은 {@link ScheduleOccurrenceExpander} 가 한다. 여기서는 후보만 좁힌다.
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
