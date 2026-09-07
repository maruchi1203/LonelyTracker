package com.lonelytracker.backend.user.dto;

import com.lonelytracker.backend.user.entity.UserEntity;

/**
 * 사용자 설정.
 * 앞으로 늘어날 설정도 이 레코드에 담는다.
 *
 * @param twoMinuteRule 2분 행동 칸을 폼에 띄울지
 */
public record UserSettingsResponse(boolean twoMinuteRule) {

    public static UserSettingsResponse from(UserEntity user) {
        return new UserSettingsResponse(user.isTwoMinuteRule());
    }
}
