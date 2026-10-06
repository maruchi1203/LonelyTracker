package com.lonelytracker.backend.habit.service;

import com.lonelytracker.backend.common.exception.ConflictException;
import com.lonelytracker.backend.common.exception.NotFoundException;
import com.lonelytracker.backend.habit.dto.HabitCategoryRequest;
import com.lonelytracker.backend.habit.dto.HabitCategoryResponse;
import com.lonelytracker.backend.habit.entity.HabitCategoryEntity;
import com.lonelytracker.backend.habit.repository.HabitCategoryRepository;
import com.lonelytracker.backend.user.entity.UserEntity;
import com.lonelytracker.backend.user.service.UserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 습관을 묶는 카테고리를 다룬다.
 * <p>
 * 습관 자체와 길을 나누는 까닭은, 카테고리 목록은 습관이 하나도 없어도 화면에 있어야 해서다.
 * 습관 응답에 이름을 실어 보내면 빈 카테고리가 화면에서 사라진다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HabitCategoryService {

    /** 아직 줄이 없다는 뜻. 어떤 카테고리도 id 가 0 일 수 없어 "나를 뺀 전부"가 전부가 된다 */
    private static final long NO_ROW = 0L;

    private final HabitCategoryRepository categoryRepository;
    private final UserProvider currentUserProvider;

    public List<HabitCategoryResponse> findAll() {
        return categoryRepository.findAllOf(currentUserProvider.get().getId()).stream()
                .map(HabitCategoryResponse::from)
                .toList();
    }

    /** 맨 뒤에 붙인다. 자리를 고르게 하지 않는 까닭은 순서를 바꾸는 길이 아직 없어서다 */
    @Transactional
    public HabitCategoryResponse create(HabitCategoryRequest request) {
        UserEntity user = currentUserProvider.get();
        String name = request.name().strip();
        requireFreeName(user.getId(), name, NO_ROW);

        return HabitCategoryResponse.from(categoryRepository.save(HabitCategoryEntity.builder()
                .user(user)
                .name(name)
                .displayOrder(categoryRepository.lastOrderOf(user.getId()) + 1)
                .build()));
    }

    /**
     * 이름만 고친다.
     * 안의 습관은 id 로 붙어 있어 건드릴 것이 없다.
     */
    @Transactional
    public HabitCategoryResponse rename(Long id, HabitCategoryRequest request) {
        HabitCategoryEntity category = getOwnedOrThrow(id);
        String name = request.name().strip();
        requireFreeName(currentUserProvider.get().getId(), name, id);

        category.rename(name);
        return HabitCategoryResponse.from(categoryRepository.saveAndFlush(category));
    }

    /**
     * 카테고리와 그 안의 습관을 통째로 지운다.
     * <p>
     * 습관을 함께 지우는 일은 테이블의 ON DELETE CASCADE 가 한다. 습관의 지난 기록도
     * habit_log 의 같은 규칙을 타고 이어서 사라진다. 화면이 먼저 몇 개가 사라지는지 알린다.
     */
    @Transactional
    public void delete(Long id) {
        HabitCategoryEntity category = getOwnedOrThrow(id);
        long userId = currentUserProvider.get().getId();
        long categoryItemCount = categoryRepository.countByUserId(userId);

        if (categoryItemCount <= 1) {
            throw new ConflictException("마지막 카테고리는 삭제할 수 없습니다");
        }

        categoryRepository.delete(category);
    }

    /** 같은 이름이 둘이면 어느 쪽에 넣었는지 사람이 가릴 수 없다 */
    private void requireFreeName(Long userId, String name, Long exceptId) {
        if (categoryRepository.existsOtherWithName(userId, name, exceptId)) {
            throw new ConflictException("이미 있는 카테고리입니다: " + name);
        }
    }

    /** 남의 카테고리는 없는 것으로 취급한다. 400을 내면 그 카테고리의 존재가 새어 나간다 */
    private HabitCategoryEntity getOwnedOrThrow(Long id) {
        return categoryRepository.findOwned(id, currentUserProvider.get().getId())
                .orElseThrow(() -> new NotFoundException("카테고리를 찾을 수 없습니다"));
    }
}
