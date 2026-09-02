package com.lonelytracker.backend.user.service;

import com.lonelytracker.backend.common.exception.CategoryNotFoundException;
import com.lonelytracker.backend.user.dto.UserCategoryAppearanceRequest;
import com.lonelytracker.backend.user.dto.UserCategoryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import com.lonelytracker.backend.user.entity.UserEntity;
import com.lonelytracker.backend.user.entity.UserCategoryEntity;
import com.lonelytracker.backend.user.repository.UserCategoryRepository;

/**
 * 사용자가 고를 수 있는 카테고리 목록을 관리한다.
 * 일정에는 이름이 문자열로 기록되므로 목록을 바꿔도 저장된 일정의 분류는 그대로다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserCategoryService {

    private final UserCategoryRepository userCategoryRepository;
    private final UserProvider currentUserProvider;

    public List<UserCategoryResponse> findAll() {
        return userCategoryRepository
                .findByUserIdOrderByDisplayOrderAscNameAsc(currentUserProvider.get().getId())
                .stream()
                .map(UserCategoryResponse::from)
                .toList();
    }

    @Transactional
    public UserCategoryResponse create(String rawName, String color) {
        UserEntity user = currentUserProvider.get();
        String name = normalize(rawName);

        if (userCategoryRepository.existsByUserIdAndName(user.getId(), name)) {
            throw new IllegalArgumentException("이미 존재하는 카테고리입니다: " + name);
        }

        return UserCategoryResponse.from(userCategoryRepository.save(UserCategoryEntity.builder()
                .user(user)
                .name(name)
                .color(color)
                .build()));
    }

    /** 목록의 이름만 바꾼다. 이미 기록된 일정의 분류는 바뀌지 않는다 */
    @Transactional
    public UserCategoryResponse rename(Long id, String rawName) {
        UserCategoryEntity category = getOrThrow(id);
        String name = normalize(rawName);

        if (name.equals(category.getName())) {
            return UserCategoryResponse.from(category);
        }
        if (userCategoryRepository.existsByUserIdAndName(category.getUser().getId(), name)) {
            throw new IllegalArgumentException("이미 존재하는 카테고리입니다: " + name);
        }

        category.rename(name);
        return UserCategoryResponse.from(category);
    }

    @Transactional
    public UserCategoryResponse updateAppearance(Long id, UserCategoryAppearanceRequest request) {
        UserCategoryEntity category = getOrThrow(id);
        category.updateAppearance(request.color(), request.displayOrder(), request.archived());
        return UserCategoryResponse.from(category);
    }

    /** 목록에서만 지운다. 이 분류를 쓰던 일정은 그대로 남는다 */
    @Transactional
    public void delete(Long id) {
        userCategoryRepository.delete(getOrThrow(id));
    }

    private String normalize(String rawName) {
        String name = rawName == null ? "" : rawName.strip();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("카테고리 이름이 비어 있습니다");
        }
        return name;
    }

    /** 다른 사용자의 카테고리는 없는 것으로 취급한다. */
    private UserCategoryEntity getOrThrow(Long id) {
        Long userId = currentUserProvider.get().getId();
        return userCategoryRepository.findById(id)
                .filter(category -> category.getUser().getId().equals(userId))
                .orElseThrow(() -> new CategoryNotFoundException("카테고리를 찾을 수 없습니다. id=" + id));
    }
}
