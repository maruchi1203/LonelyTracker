package com.lonelytracker.backend.user.service;

import com.lonelytracker.backend.common.exception.UserNotFoundException;
import com.lonelytracker.backend.common.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import com.lonelytracker.backend.user.entity.UserEntity;
import com.lonelytracker.backend.user.repository.UserRepository;

/**
 * 지금 요청을 보낸 사용자를 돌려준다.
 * 인증이 없어 application.yml 의 기본 계정을 항상 내주는 임시 구현이다.
 */
@Component
@RequiredArgsConstructor
public class UserProvider {

    private final UserRepository userRepository;
    private final AppProperties appProperties;

    /** @throws UserNotFoundException 기본 계정이 DB에 없을 때 */
    public UserEntity get() {
        String username = appProperties.user().defaultUsername();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(
                        "기본 사용자(" + username + ")를 찾을 수 없습니다. 마이그레이션이 적용됐는지 확인하세요"));
    }
}
