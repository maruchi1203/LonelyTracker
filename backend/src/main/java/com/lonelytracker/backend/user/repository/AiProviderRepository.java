package com.lonelytracker.backend.user.repository;

import com.lonelytracker.backend.user.entity.AiProviderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** 사용자별 AI 제공자 설정. 주소마다 한 줄이다 */
public interface AiProviderRepository extends JpaRepository<AiProviderEntity, Long> {

    List<AiProviderEntity> findAllByUserIdOrderByCreatedAtAsc(Long userId);

    Optional<AiProviderEntity> findByIdAndUserId(Long id, Long userId);

    Optional<AiProviderEntity> findByUserIdAndBaseUrl(Long userId, String baseUrl);
}
