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
     * <p>
     * onDate 와 startAt 을 <b>OR</b> 로 봐야 한다. 8/31 을 9/2 로 미룬 회차는
     * onDate 로는 8월에, startAt 으로는 9월에 잡히고 <b>두 달 모두에 보여야</b> 하기 때문이다.
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
     * 일정 삭제에 앞서 회차 기록을 먼저 지운다.
     * <p>
     * DB 에 {@code ON DELETE CASCADE} 가 걸려 있지만 <b>Hibernate 는 그것을 모른다</b>.
     * 영속성 컨텍스트에 회차가 남은 채 일정을 지우면 flush 때
     * {@code TransientPropertyValueException} 이 난다.
     * <p>
     * 벌크 @Query 대신 파생 삭제를 쓰는 이유도 같다. 벌크는 DB 만 바꾸고
     * 영속성 컨텍스트를 그대로 두므로 같은 문제가 남는다.
     */
    void deleteByScheduleId(Long scheduleId);

    /**
     * 그만두기 시 앞으로의 기록을 지운다.
     * <p>
     * 규칙의 종료일만 당기면, <b>미뤄둔 미래 회차가 되살아난다</b>.
     * 전개기가 "범위 밖에서 미뤄져 들어온 회차" 를 따로 잡아내기 때문이다.
     * onDate 가 오늘 이후이거나 옮겨간 시각이 오늘 이후인 기록을 함께 지운다.
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
