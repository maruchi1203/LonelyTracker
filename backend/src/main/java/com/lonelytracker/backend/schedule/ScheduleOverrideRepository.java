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
     * 시리즈 전체 삭제에 앞서 override 를 먼저 지운다.
     * <p>
     * DB 에 {@code ON DELETE CASCADE} 가 걸려 있지만 <b>Hibernate 는 그것을 모른다</b>.
     * 영속성 컨텍스트에 override 가 남은 채 시리즈를 지우면 flush 때
     * {@code TransientPropertyValueException} 이 난다 - 없어진 것을 가리킨다는 이유다.
     * <p>
     * 벌크 @Query 대신 파생 삭제를 쓰는 이유도 같다. 벌크는 DB 만 바꾸고
     * 영속성 컨텍스트를 그대로 두므로 같은 문제가 남는다.
     */
    void deleteBySeriesId(Long seriesId);

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
