package com.lonelytracker.backend.user.service;

import com.lonelytracker.backend.common.exception.UserNotFoundException;
import com.lonelytracker.backend.common.AppProperties;
import com.lonelytracker.backend.user.dto.OpenAiKeyStatus;
import com.lonelytracker.backend.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.lonelytracker.backend.user.entity.UserEntity;
import com.lonelytracker.backend.user.entity.UserCategoryEntity;
import com.lonelytracker.backend.user.repository.UserCategoryRepository;
import com.lonelytracker.backend.user.repository.UserRepository;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserCategoryRepository userCategoryRepository;
    private final AppProperties appProperties;
    private final UserProvider currentUserProvider;

    public UserResponse findById(Long id) {
        return UserResponse.from(userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다. id=" + id)));
    }

    /**
     * 사용자 생성
     * 1. 
     */
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

    @Transactional
    public UserResponse create(String username, String displayName) {
        String name = username == null ? "" : username.strip();

        // username 빈칸 오류 처리
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

        seedRecommendedCategories(user);

        return UserResponse.from(user);
    }

    /** 추천 목록은 application.yml 의 lonelytracker.user.recommended-categories 에 있다. */
    private void seedRecommendedCategories(UserEntity user) {
        int order = 0;
        for (String categoryName : appProperties.user().recommendedCategories()) {
            userCategoryRepository.save(UserCategoryEntity.builder()
                    .user(user)
                    .name(categoryName)
                    .displayOrder(order++)
                    .build());
        }
    }
}
