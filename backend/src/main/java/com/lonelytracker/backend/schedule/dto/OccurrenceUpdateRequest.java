package com.lonelytracker.backend.schedule.dto;

import com.lonelytracker.backend.common.FieldLengths;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 단일 회차 수정하는 DTO
 */
public record OccurrenceUpdateRequest(
                @Size(max = FieldLengths.TITLE, message = "title은 200자를 넘을 수 없습니다") String title,

                @Size(max = FieldLengths.DESCRIPTION, message = "description은 20000자를 넘을 수 없습니다") String description,

                LocalDateTime startAt,

                LocalDateTime endAt,

                @Size(max = FieldLengths.CATEGORY_NAME, message = "category는 50자를 넘을 수 없습니다") String category) {
}
