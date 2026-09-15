package com.lonelytracker.backend.user.service;

import com.lonelytracker.backend.ai.AiUsage;
import com.lonelytracker.backend.user.dto.AiUsageSummaryResponse;
import com.lonelytracker.backend.user.entity.AiUsageEntity;
import com.lonelytracker.backend.user.entity.UserEntity;
import com.lonelytracker.backend.user.repository.AiUsageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

/**
 * AI 호출 기록을 적고 기간별로 모은다.
 * 제공자의 사용량 API 는 관리자 키가 필요하고 제공자마다 달라, 응답에 실려 온 토큰을 직접 적는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiUsageService {

    /** 이보다 오래된 기록은 지운다 */
    static final Period RETENTION = Period.ofMonths(3);

    private final AiUsageRepository usageRepository;
    private final UserProvider currentUserProvider;

    /** 호출 한 번을 적고, 석 달이 지난 기록을 지운다 */
    @Transactional
    public void record(String baseUrl, String model, AiUsage usage) {
        UserEntity user = currentUserProvider.get();

        usageRepository.save(AiUsageEntity.builder()
                .user(user)
                .baseUrl(providerKeyOf(baseUrl))
                .model(model)
                .inputTokens(usage.inputTokens())
                .outputTokens(usage.outputTokens())
                .build());
        usageRepository.deleteByUser_IdAndCreatedAtBefore(user.getId(), LocalDateTime.now().minus(RETENTION));
    }

    public AiUsageSummaryResponse summary() {
        Long userId = currentUserProvider.get().getId();
        LocalDate today = LocalDate.now();

        return new AiUsageSummaryResponse(
                usageRepository.sumByProvider(userId, today.with(DayOfWeek.MONDAY).atStartOfDay()),
                usageRepository.sumByProvider(userId, today.withDayOfMonth(1).atStartOfDay()));
    }

    /**
     * 자격 증명과 .env 가 같은 제공자를 다르게 적어도 한 줄로 모이게 한다
     * .env 값은 검증을 거치지 않아 맞출 수 없으면 그대로 둔다
     */
    private static String providerKeyOf(String baseUrl) {
        try {
            return AiCredentialService.normalizeBaseUrl(baseUrl);
        } catch (IllegalArgumentException e) {
            return baseUrl;
        }
    }
}
