package com.lonelytracker.backend.schedule.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import com.lonelytracker.backend.schedule.domain.SchedulePriority;
import com.lonelytracker.backend.schedule.entity.ScheduleEntity;

public interface ScheduleRepository extends JpaRepository<ScheduleEntity, Long> {
  /**
   * 조회 범위에 회차를 낼 수 있는 일정 후보를 모은다.
   * 반복 일정, 범위 안의 1회성 일정, 미뤄서 범위로 들어온 회차를 가진 일정을 OR로 묶는다.
   * 정확한 날짜 판정은 전개기가 하고 여기서는 후보만 좁힌다.
   * <p>
   * 안 하기로 한 일정(WONT)은 뺀다. 시간축을 차지할 이유가 없다.
   *
   * @param from 조회 시작 시각
   * @param to   조회 끝 시각
   */
  @Query("""
      select s from ScheduleEntity s
      where s.user.id = :userId
        and (s.priority is null or s.priority <> :wont)
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
      @Param("wont") SchedulePriority wont,
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to,
      @Param("fromDate") LocalDate fromDate,
      @Param("toDate") LocalDate toDate);

  /**
   * 규칙이 붙은 일정을 전부 모은다. 끝난 반복도 함께 온다.
   * 회차가 아니라 일정 자체라 조회 구간이 필요 없다.
   */
  @Query("""
      select s from ScheduleEntity s
      where s.user.id = :userId
        and exists (select 1 from ScheduleRecurEntity r where r.scheduleId = s.id)
      order by s.startAt desc
      """)
  List<ScheduleEntity> findRecurring(@Param("userId") Long userId);

  /**
   * 이미 쓴 적 있는 태그 이름. 자동완성 후보가 된다.
   * 따로 목록을 관리하지 않아 후보와 실제가 어긋나지 않는다.
   */
  @Query("""
      select distinct t from ScheduleEntity s
      join s.tags t
      where s.user.id = :userId
      order by t
      """)
  List<String> findTagNames(@Param("userId") Long userId);

  /**
   * 리스트 탭이 보는 일정. 습관을 포함해 전부 가져온다.
   * 회차가 아니라 일정 자체라 조회 구간이 없다.
   * 정렬은 서비스가 맡는다. 기한이 없으면 시작일시를 쓰는 규칙을 JPQL로 쓰기 어렵다.
   */
  @Query("""
      select s from ScheduleEntity s
      where s.user.id = :userId
      """)
  List<ScheduleEntity> findForList(@Param("userId") Long userId);

  /**
   * 한 형제 무리 전부. 순서를 다시 매길 때 쓴다.
   * 리스트가 보는 것과 무리가 같아야 한다. 하나라도 어긋나면 재정렬이 늘 거절된다.
   *
   * @param parentId null이면 최상위 무리다
   */
  @Query("""
      select s from ScheduleEntity s
      where s.user.id = :userId
        and ((:parentId is null and s.parentId is null) or s.parentId = :parentId)
      """)
  List<ScheduleEntity> findSiblings(@Param("userId") Long userId,
      @Param("parentId") Long parentId);

  /**
   * 주어진 일정들의 자식 id. 계층의 깊이를 잴 때 쓴다.
   * 3단까지라 두 번 부르면 손자까지 닿는다.
   */
  @Query("select s.id from ScheduleEntity s where s.parentId in :parentIds")
  List<Long> findIdsByParentIdIn(@Param("parentIds") Collection<Long> parentIds);
}
