package com.lonelytracker.backend.user.service;

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

import java.net.URI;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 제공자별 자격 증명을 다룬다.
 * 키 원본은 {@link #resolve()} 로 파서에 넘길 때만 나가고 어떤 응답에도 실리지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AiCredentialService {

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
        String baseUrl = normalizeBaseUrl(request.baseUrl());
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

    /**
     * 같은 제공자가 두 줄로 갈리지 않도록 주소를 한 모양으로 맞춘다
     *
     * @throws IllegalArgumentException 주소로 쓸 수 없을 때
     */
    static String normalizeBaseUrl(String raw) {
        String url = (raw == null) ? "" : raw.strip();

        // 1. 쪼개기. 따옴표나 가운데 공백이 있으면 여기서 걸린다
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("주소 형식이 올바르지 않습니다");
        }

        // 2. 거절
        String scheme = (uri.getScheme() == null) ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new IllegalArgumentException("http 또는 https 로 시작하는 주소를 넣어 주세요");
        }
        if (uri.getHost() == null) {
            // 적긴 했는데 호스트로 읽히지 않은 것. 밑줄 같은 문자가 원인이다
            if (uri.getRawAuthority() != null) {
                throw new IllegalArgumentException(
                        "호스트 이름에 밑줄(_) 같은 문자는 쓸 수 없습니다. 하이픈(-)으로 바꾸거나 IP 주소를 넣어 주세요");
            }
            throw new IllegalArgumentException("주소에 호스트가 없습니다");
        }
        if (uri.getRawUserInfo() != null) {
            throw new IllegalArgumentException("주소에 계정 정보를 넣을 수 없습니다");
        }
        if (uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new IllegalArgumentException("주소에 ? 나 # 를 붙일 수 없습니다");
        }

        // 3. 맞추기 — 호스트만 소문자로
        String host = uri.getHost().toLowerCase(Locale.ROOT);

        // 기본 포트면 버리고, 아니면 그대로 붙인다
        int port = uri.getPort();
        boolean defaultPort = port == -1
                || (scheme.equals("https") && port == 443)
                || (scheme.equals("http") && port == 80);
        String portPart = defaultPort ? "" : ":" + port;

        // 경로는 대소문자 그대로. 끝의 / 만 전부 뗀다
        String path = (uri.getRawPath() == null) ? "" : uri.getRawPath();
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        // 4. 조립
        return scheme + "://" + host + portPart + path;
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
