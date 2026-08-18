package com.lonelytracker.backend.category;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByPath(String path);

    boolean existsByPath(String path);

    /** 사이드바 표시용. 경로순으로 주면 부모가 항상 자식보다 먼저 온다. */
    List<Category> findAllByOrderByPathAsc();

    /**
     * 이름 변경 시 함께 갱신해야 하는 후손들.
     * ESCAPE를 지정하는 이유는 PostgreSQL이 LIKE에서 역슬래시를 기본 이스케이프로
     * 취급하는데, 우리 구분자가 역슬래시라 충돌하기 때문이다.
     */
    @Query("select c from Category c where c.path like :pattern escape '!'")
    List<Category> findDescendants(@Param("pattern") String pattern);
}
