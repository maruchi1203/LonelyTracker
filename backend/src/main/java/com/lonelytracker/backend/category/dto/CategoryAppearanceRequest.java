package com.lonelytracker.backend.category.dto;

import jakarta.validation.constraints.Size;

/** 넘긴 항목만 반영한다. null은 "변경 없음"을 뜻한다. */
public record CategoryAppearanceRequest(
        @Size(max = 20, message = "color는 20자를 넘을 수 없습니다")
        String color,
        Integer displayOrder,
        Boolean collapsed,
        Boolean archived
) {
}
