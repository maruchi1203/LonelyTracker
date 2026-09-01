package com.lonelytracker.backend.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import com.lonelytracker.backend.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}
