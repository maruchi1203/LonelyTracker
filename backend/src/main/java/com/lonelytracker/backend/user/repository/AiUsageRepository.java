package com.lonelytracker.backend.user.repository;

import com.lonelytracker.backend.user.dto.ProviderUsageResponse;
import com.lonelytracker.backend.user.entity.AiUsageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

/** AI 호출 기록. 한 사용자 안에서 기간과 제공자로 모은다 */
public interface AiUsageRepository extends JpaRepository<AiUsageEntity, Long> {

    /** 그 시각 이후의 사용량을 제공자별로 모은다. 호출이 많은 제공자가 앞 */
    @Query("""
            select new com.lonelytracker.backend.user.dto.ProviderUsageResponse(
                u.baseUrl, count(u), sum(u.inputTokens), sum(u.outputTokens))
            from AiUsageEntity u
            where u.user.id = :userId and u.createdAt >= :since
            group by u.baseUrl
            order by count(u) desc, u.baseUrl
            """)
    List<ProviderUsageResponse> sumByProvider(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    void deleteByUserIdAndCreatedAtBefore(Long userId, LocalDateTime before);
}
