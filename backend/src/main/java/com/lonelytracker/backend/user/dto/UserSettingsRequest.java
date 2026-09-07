package com.lonelytracker.backend.user.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 사용자 설정 변경.
 *
 * @param twoMinuteRule 2분 행동 칸을 폼에 띄울지
 */
public record UserSettingsRequest(
                @NotNull(message = "twoMinuteRule은 필수입니다") Boolean twoMinuteRule) {
}
