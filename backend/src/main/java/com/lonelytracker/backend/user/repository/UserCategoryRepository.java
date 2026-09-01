package com.lonelytracker.backend.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import com.lonelytracker.backend.user.entity.UserCategory;

public interface UserCategoryRepository extends JpaRepository<UserCategory, Long> {

    /** 사이드바 표시 순서. displayOrder 가 같으면 이름순으로 안정적으로 정렬한다. */
    List<UserCategory> findByUserIdOrderByDisplayOrderAscNameAsc(Long userId);

    Optional<UserCategory> findByUserIdAndName(Long userId, String name);

    boolean existsByUserIdAndName(Long userId, String name);
}
