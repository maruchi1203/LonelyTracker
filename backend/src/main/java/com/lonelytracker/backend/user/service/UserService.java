package com.lonelytracker.backend.user.service;

import com.lonelytracker.backend.common.exception.UserNotFoundException;
import com.lonelytracker.backend.common.AppProperties;
import com.lonelytracker.backend.user.dto.OpenAiKeyStatus;
import com.lonelytracker.backend.user.dto.UserResponse;
import com.lonelytracker.backend.user.dto.UserSettingsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.lonelytracker.backend.user.entity.UserEntity;
import com.lonelytracker.backend.user.repository.UserRepository;


/**
 * 사용자 계정과 설정, OpenAI API 키를 다룬다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserProvider currentUserProvider;

    public UserResponse findById(Long id) {
        return UserResponse.from(userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다. id=" + id)));
    }

    /** 키 등록 여부만 돌려준다. 원본은 어떤 경로로도 나가지 않는다. */
    public OpenAiKeyStatus openAiKeyStatus() {
        return OpenAiKeyStatus.of(currentUserProvider.get().getOpenAiApiKey());
    }

    /** null 이나 빈 값을 주면 등록을 해제한다. */
    @Transactional
    public OpenAiKeyStatus changeOpenAiKey(String apiKey) {
        UserEntity user = currentUserProvider.get();
        user.changeOpenAiApiKey(apiKey);
        userRepository.saveAndFlush(user);
        return OpenAiKeyStatus.of(user.getOpenAiApiKey());
    }

    /** 사용자 설정을 돌려준다. */
    public UserSettingsResponse settings() {
        return UserSettingsResponse.from(currentUserProvider.get());
    }

    /** 사용자 설정을 바꾼다. */
    @Transactional
    public UserSettingsResponse changeSettings(boolean twoMinuteRule) {
        UserEntity user = currentUserProvider.get();
        user.changeTwoMinuteRule(twoMinuteRule);
        userRepository.saveAndFlush(user);
        return UserSettingsResponse.from(user);
    }

    /** 사용자를 만든다. */
    @Transactional
    public UserResponse create(String username, String displayName) {
        String name = username == null ? "" : username.strip();

        if (name.isEmpty()) {
            throw new IllegalArgumentException("username이 비어 있습니다");
        }

        if (userRepository.existsByUsername(name)) {
            throw new IllegalArgumentException("이미 존재하는 사용자입니다: " + name);
        }

        UserEntity user = userRepository.save(UserEntity.builder()
                .username(name)
                .displayName(displayName == null || displayName.isBlank() ? name : displayName.strip())
                .build());

        return UserResponse.from(user);
    }
}
