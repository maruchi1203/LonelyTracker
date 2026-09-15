package com.lonelytracker.backend.user.service;

import com.lonelytracker.backend.ai.AiBaseUrls;
import com.lonelytracker.backend.ai.AiTarget;
import com.lonelytracker.backend.ai.AiTargetResolver;
import com.lonelytracker.backend.common.AppProperties;
import com.lonelytracker.backend.common.exception.AiUnavailableException;
import com.lonelytracker.backend.common.exception.NotFoundException;
import com.lonelytracker.backend.user.dto.AiCredentialListResponse;
import com.lonelytracker.backend.user.dto.AiCredentialRequest;
import com.lonelytracker.backend.user.dto.AiCredentialResponse;
import com.lonelytracker.backend.user.entity.AiCredentialEntity;
import com.lonelytracker.backend.user.entity.UserEntity;
import com.lonelytracker.backend.user.repository.AiCredentialRepository;
import com.lonelytracker.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 제공자별 자격 증명을 다룬다.
 * 키 원본은 {@link #resolve()} 로 파서에 넘길 때만 나가고 어떤 응답에도 실리지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiCredentialService implements AiTargetResolver {

    private final AiCredentialRepository credentialRepository;
    private final UserRepository userRepository;
    private final UserProvider currentUserProvider;
    private final AppProperties properties;

    public AiCredentialListResponse list() {
        UserEntity user = currentUserProvider.get();
        return new AiCredentialListResponse(
                credentialRepository.findAllByUser_IdOrderByCreatedAtAsc(user.getId()).stream()
                        .map(c -> AiCredentialResponse.of(c, user.getActiveAiCredentialId()))
                        .toList(),
                properties.ai().hasApiKey());
    }

    /**
     * 주소가 같은 줄이 있으면 고치고 없으면 만든다. 저장한 것을 바로 쓰는 것으로 고른다
     */
    @Transactional
    public AiCredentialResponse save(AiCredentialRequest request) {
        UserEntity user = currentUserProvider.get();
        String baseUrl = AiBaseUrls.normalize(request.baseUrl());
        String model = request.model().strip();
        String apiKey = (request.apiKey() == null) ? "" : request.apiKey().strip();

        AiCredentialEntity credential = credentialRepository
                .findByUser_IdAndBaseUrl(user.getId(), baseUrl)
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
                    return AiCredentialEntity.builder()
                            .user(user).baseUrl(baseUrl).model(model).apiKey(apiKey).build();
                });

        credentialRepository.saveAndFlush(credential);
        user.changeActiveAiCredential(credential.getId());
        userRepository.saveAndFlush(user);
        return AiCredentialResponse.of(credential, credential.getId());
    }

    @Transactional
    public AiCredentialResponse activate(Long id) {
        UserEntity user = currentUserProvider.get();
        AiCredentialEntity credential = getOwnedOrThrow(id, user);
        user.changeActiveAiCredential(credential.getId());
        userRepository.saveAndFlush(user);
        return AiCredentialResponse.of(credential, credential.getId());
    }

    /** 쓰던 것을 지우면 서버 설정으로 돌아간다 */
    @Transactional
    public void delete(Long id) {
        UserEntity user = currentUserProvider.get();
        AiCredentialEntity credential = getOwnedOrThrow(id, user);

        // DB 의 SET NULL 을 Hibernate 가 모르므로 엔티티에서도 끊는다
        if (credential.getId().equals(user.getActiveAiCredentialId())) {
            user.changeActiveAiCredential(null);
            userRepository.saveAndFlush(user);
        }
        credentialRepository.delete(credential);
    }

    /**
     * 이번 파싱이 부를 곳. 고른 자격 증명이 먼저고, 없으면 서버 설정이다
     *
     * @throws AiUnavailableException 둘 다 없을 때
     */
    @Override
    public AiTarget resolve() {
        UserEntity user = currentUserProvider.get();
        Long activeId = user.getActiveAiCredentialId();

        if (activeId != null) {
            return credentialRepository.findByIdAndUser_Id(activeId, user.getId())
                    .map(c -> new AiTarget(c.getBaseUrl(), c.getModel(), c.getApiKey()))
                    .orElseThrow(AiCredentialService::noKey);
        }

        AppProperties.AiSetting server = properties.ai();
        if (server.hasApiKey()) {
            return new AiTarget(server.baseUrl(), server.model(), server.apiKey());
        }
        throw noKey();
    }

    private AiCredentialEntity getOwnedOrThrow(Long id, UserEntity user) {
        return credentialRepository.findByIdAndUser_Id(id, user.getId())
                .orElseThrow(() -> new NotFoundException("자격 증명을 찾을 수 없습니다. id=" + id));
    }

    // 서버 설정이 아니라 이 사용자가 키를 넣지 않은 것이다
    private static AiUnavailableException noKey() {
        return new AiUnavailableException(
                "AI API 키를 먼저 등록해 주세요. 등록 전에는 직접 입력해 주세요");
    }
}
