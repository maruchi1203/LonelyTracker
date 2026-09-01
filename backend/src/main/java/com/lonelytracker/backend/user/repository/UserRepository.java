package com.lonelytracker.backend.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import com.lonelytracker.backend.user.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findByUsername(String username);

    boolean existsByUsername(String username);
}
