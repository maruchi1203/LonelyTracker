package com.lonelytracker.backend.habit.repository;

import com.lonelytracker.backend.habit.entity.HabitCategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface HabitCategoryRepository extends JpaRepository<HabitCategoryEntity, Long> {

    /** 그 사람의 갈래 전부. 화면에 늘어놓을 차례로 온다 */
    @Query("select c from HabitCategoryEntity c where c.user.id = :userId"
            + " order by c.displayOrder, c.id")
    List<HabitCategoryEntity> findAllOf(@Param("userId") Long userId);

    /** 남의 갈래를 건드리지 못하게 소유자까지 함께 본다 */
    @Query("select c from HabitCategoryEntity c where c.id = :id and c.user.id = :userId")
    Optional<HabitCategoryEntity> findOwned(@Param("id") Long id, @Param("userId") Long userId);

    /** 이름이 겹치는지. 테이블의 UNIQUE 와 같은 것을 보되 먼저 걸러 말로 알린다 */
    @Query("select count(c) > 0 from HabitCategoryEntity c"
            + " where c.user.id = :userId and c.name = :name and c.id <> :exceptId")
    boolean existsOtherWithName(@Param("userId") Long userId, @Param("name") String name,
            @Param("exceptId") Long exceptId);

    /** 새 갈래를 맨 뒤에 붙이기 위한 자리. 갈래가 없으면 -1 이 와서 첫 자리가 0 이 된다 */
    @Query("select coalesce(max(c.displayOrder), -1) from HabitCategoryEntity c"
            + " where c.user.id = :userId")
    int lastOrderOf(@Param("userId") Long userId);

    long countByUserId(Long userId);
}
