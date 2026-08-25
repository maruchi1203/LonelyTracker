package com.lonelytracker.backend.user;

import com.lonelytracker.backend.common.exception.UserNotFoundException;
import com.lonelytracker.backend.common.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurrentUserProvider {

    private final UserRepository userRepository;
    private final AppProperties appProperties;

    public User get() {
        String username = appProperties.user().defaultUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(
                        "기본 사용자(" + username + ")를 찾을 수 없습니다. 마이그레이션이 적용됐는지 확인하세요"));
    }
}
