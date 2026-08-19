package com.lonelytracker.backend.user;

import com.lonelytracker.backend.common.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 현재 요청의 사용자를 돌려준다.
 * <p>
 * <b>임시 구현이다.</b> 아직 인증이 없어 설정에 적힌 기본 사용자를 항상 반환한다.
 * 로그인(OAuth)이 붙으면 이 클래스만 바꾸면 되고, 서비스·컨트롤러는 손대지 않아도 된다.
 * 소유자 개념을 지금부터 코드 전반에 흘려두는 것이 목적이다.
 */
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
