package com.lonelytracker.backend.user.repository;

import com.lonelytracker.backend.user.entity.AiCredentialEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** 사용자별 AI 자격 증명. 주소마다 한 줄이다 */
public interface AiCredentialRepository extends JpaRepository<AiCredentialEntity, Long> {

    List<AiCredentialEntity> findAllByUser_IdOrderByCreatedAtAsc(Long userId);

    Optional<AiCredentialEntity> findByIdAndUser_Id(Long id, Long userId);

    Optional<AiCredentialEntity> findByUser_IdAndBaseUrl(Long userId, String baseUrl);
}
