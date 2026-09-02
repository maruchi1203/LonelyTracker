package com.lonelytracker.backend.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import com.lonelytracker.backend.user.entity.UserEntity;

/** 사용자 조회. username 은 전체에서 유일하다 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    boolean existsByUsername(String username);
}
