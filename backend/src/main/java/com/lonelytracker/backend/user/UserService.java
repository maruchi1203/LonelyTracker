package com.lonelytracker.backend.user;

import com.lonelytracker.backend.common.AppProperties;
import com.lonelytracker.backend.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserCategoryRepository userCategoryRepository;
    private final AppProperties appProperties;

    public UserResponse findById(Long id) {
        return UserResponse.from(userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다. id=" + id)));
    }

    /**
     * 사용자를 만들고 추천 카테고리를 함께 넣는다.
     * 둘이 한 트랜잭션이라, 카테고리 생성이 실패하면 사용자도 남지 않는다.
     */
    @Transactional
    public UserResponse create(String username, String displayName) {
        String name = username == null ? "" : username.strip();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("username이 비어 있습니다");
        }
        if (userRepository.existsByUsername(name)) {
            throw new IllegalArgumentException("이미 존재하는 사용자입니다: " + name);
        }

        User user = userRepository.save(User.builder()
                .username(name)
                .displayName(displayName == null || displayName.isBlank() ? name : displayName.strip())
                .build());

        seedRecommendedCategories(user);

        return UserResponse.from(user);
    }

    /** 추천 목록은 application.yml 의 lonelytracker.user.recommended-categories 에 있다. */
    private void seedRecommendedCategories(User user) {
        int order = 0;
        for (String categoryName : appProperties.user().recommendedCategories()) {
            userCategoryRepository.save(UserCategory.builder()
                    .user(user)
                    .name(categoryName)
                    .displayOrder(order++)
                    .build());
        }
    }
}
