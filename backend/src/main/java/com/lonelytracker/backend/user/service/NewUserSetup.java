package com.lonelytracker.backend.user.service;

import com.lonelytracker.backend.user.entity.UserEntity;

/**
 * 새 사용자를 쓸 수 있는 상태로 채워 두는 계약
 */
public interface NewUserSetup {

    /** @param user 방금 저장된 사용자 */
    void prepare(UserEntity user);
}
