package com.lonelytracker.backend.user.service;

import com.lonelytracker.backend.ai.AiBaseUrls;
import com.lonelytracker.backend.ai.AiTarget;
import com.lonelytracker.backend.ai.AiTargetResolver;
import com.lonelytracker.backend.common.AppProperties;
import com.lonelytracker.backend.common.exception.AiUnavailableException;
import com.lonelytracker.backend.common.exception.NotFoundException;
import com.lonelytracker.backend.user.dto.AiProviderListResponse;
import com.lonelytracker.backend.user.dto.AiProviderRequest;
import com.lonelytracker.backend.user.dto.AiProviderResponse;
import com.lonelytracker.backend.user.entity.AiProviderEntity;
import com.lonelytracker.backend.user.entity.UserEntity;
import com.lonelytracker.backend.user.repository.AiProviderRepository;
import com.lonelytracker.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 제공자별 제공자 설정을 다룬다.
 * 키 원본은 {@link #resolve()} 로 파서에 넘길 때만 나가고 어떤 응답에도 실리지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiProviderService implements AiTargetResolver {

    private final AiProviderRepository providerRepository;
    private final UserRepository userRepository;
    private final UserProvider currentUserProvider;
    private final AppProperties properties;

    public AiProviderListResponse list() {
        UserEntity user = currentUserProvider.get();
        return new AiProviderListResponse(
                providerRepository.findAllByUserIdOrderByCreatedAtAsc(user.getId()).stream()
                        .map(c -> AiProviderResponse.of(c, user.getActiveAiProviderId()))
                        .toList(),
                properties.ai().hasApiKey());
    }

    /**
     * 주소가 같은 줄이 있으면 고치고 없으면 만든다. 저장한 것을 바로 쓰는 것으로 고른다
     */
    @Transactional
    public AiProviderResponse save(AiProviderRequest request) {
        UserEntity user = currentUserProvider.get();
        String baseUrl = AiBaseUrls.normalize(request.baseUrl());
        String model = request.model().strip();
        String apiKey = (request.apiKey() == null) ? "" : request.apiKey().strip();

        AiProviderEntity provider = providerRepository
                .findByUserIdAndBaseUrl(user.getId(), baseUrl)
                .map(existing -> {
                    existing.changeModel(model);
                    // 모델만 바꿀 때 키를 다시 넣게 하지 않는다
                    if (!apiKey.isEmpty()) {
                        existing.changeApiKey(apiKey);
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    if (apiKey.isEmpty()) {
                        throw new IllegalArgumentException("API 키를 넣어 주세요");
                    }
                    return AiProviderEntity.builder()
                            .user(user).baseUrl(baseUrl).model(model).apiKey(apiKey).build();
                });

        providerRepository.saveAndFlush(provider);
        user.changeActiveAiProvider(provider.getId());
        userRepository.saveAndFlush(user);
        return AiProviderResponse.of(provider, provider.getId());
    }

    @Transactional
    public AiProviderResponse activate(Long id) {
        UserEntity user = currentUserProvider.get();
        AiProviderEntity provider = getOwnedOrThrow(id, user);
        user.changeActiveAiProvider(provider.getId());
        userRepository.saveAndFlush(user);
        return AiProviderResponse.of(provider, provider.getId());
    }

    /** 쓰던 것을 지우면 서버 설정으로 돌아간다 */
    @Transactional
    public void delete(Long id) {
        UserEntity user = currentUserProvider.get();
        AiProviderEntity provider = getOwnedOrThrow(id, user);

        // DB 의 SET NULL 을 Hibernate 가 모르므로 엔티티에서도 끊는다
        if (provider.getId().equals(user.getActiveAiProviderId())) {
            user.changeActiveAiProvider(null);
            userRepository.saveAndFlush(user);
        }
        providerRepository.delete(provider);
    }

    /**
     * 이번 파싱이 부를 곳. 고른 제공자 설정이 먼저고, 없으면 서버 설정이다
     *
     * @throws AiUnavailableException 둘 다 없을 때
     */
    @Override
    public AiTarget resolve() {
        UserEntity user = currentUserProvider.get();
        Long activeId = user.getActiveAiProviderId();

        if (activeId != null) {
            return providerRepository.findByIdAndUserId(activeId, user.getId())
                    .map(c -> new AiTarget(c.getBaseUrl(), c.getModel(), c.getApiKey()))
                    .orElseThrow(AiProviderService::noKey);
        }

        AppProperties.AiSetting server = properties.ai();
        if (server.hasApiKey()) {
            return new AiTarget(server.baseUrl(), server.model(), server.apiKey());
        }
        throw noKey();
    }

    private AiProviderEntity getOwnedOrThrow(Long id, UserEntity user) {
        return providerRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new NotFoundException("제공자 설정을 찾을 수 없습니다. id=" + id));
    }

    // 서버 설정이 아니라 이 사용자가 키를 넣지 않은 것이다
    private static AiUnavailableException noKey() {
        return new AiUnavailableException(
                "AI API 키를 먼저 등록해 주세요. 등록 전에는 직접 입력해 주세요");
    }
}
