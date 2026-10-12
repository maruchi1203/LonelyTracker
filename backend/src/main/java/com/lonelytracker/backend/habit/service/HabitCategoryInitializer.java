package com.lonelytracker.backend.habit.service;

import com.lonelytracker.backend.habit.entity.HabitCategoryEntity;
import com.lonelytracker.backend.habit.repository.HabitCategoryRepository;
import com.lonelytracker.backend.user.entity.UserEntity;
import com.lonelytracker.backend.user.service.NewUserSetup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.IntStream;

/**
 * 새 사용자에게 기본 카테고리를 심는다.
 * <p>
 * habit.category_id 가 NOT NULL 이라 카테고리가 없으면 습관을 하나도 만들 수 없다.
 */
@Component
@RequiredArgsConstructor
public class HabitCategoryInitializer implements NewUserSetup {

        /** 처음 보이는 카테고리. 사용자가 고치고 지운다 */
        private static final List<String> DEFAULTS = List.of("운동", "마음챙김", "부업", "예술", "학습", "인간관계");

        private final HabitCategoryRepository categoryRepository;

        @Override
        public void prepare(UserEntity user) {
                categoryRepository.saveAll(IntStream.range(0, DEFAULTS.size())
                                .mapToObj(order -> HabitCategoryEntity.builder()
                                                .user(user)
                                                .name(DEFAULTS.get(order))
                                                .displayOrder(order)
                                                .build())
                                .toList());
        }
}
