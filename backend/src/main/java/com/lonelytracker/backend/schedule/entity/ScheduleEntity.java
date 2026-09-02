package com.lonelytracker.backend.schedule.entity;

import com.lonelytracker.backend.common.FieldLengths;
import com.lonelytracker.backend.user.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 단일·반복 일정의 공통 정보.
 * 반복 규칙은 {@link ScheduleRecurEntity},
 * 회차별 수행은 {@link ScheduleProgressEntity}에 있다.
 */
@Entity
@Table(name = "schedule", indexes = {
        @Index(name = "idx_schedule_start_at", columnList = "start_at"),
        @Index(name = "idx_schedule_category", columnList = "category"),
        @Index(name = "idx_schedule_user_id", columnList = "user_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ScheduleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 제목
     */
    @Column(nullable = false, length = FieldLengths.TITLE)
    private String title;

    /**
     * 설명 (마크다운 원문)
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 시작일시
     * 반복 일정이면 첫 회차의 일시다
     */
    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    /**
     * 소요시간(분). null이면 종료 시각 없음
     * 회차의 종료는 그 회차의 startAt + durationMinutes 로 구한다
     */
    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    /**
     * 하루 종일 여부
     */
    @Column(name = "all_day", nullable = false)
    private boolean allDay;

    /**
     * 소유자 {@link UserEntity}
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    /**
     * 일정 카테고리
     * 목록과 FK로 묶지 않아 분류를 지워도 기록은 남는다
     */
    @Column(length = FieldLengths.CATEGORY_NAME)
    private String category;

    /**
     * 등록일시
     */
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 수정일시
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 일정 정보를 변경한다.
     *
     * @param title           제목
     * @param description     설명
     * @param startAt         시작일시
     * @param durationMinutes 소요시간(분)
     * @param allDay          하루 종일 여부
     * @param category        일정 카테고리
     */
    public void update(String title, String description, LocalDateTime startAt,
            Integer durationMinutes, boolean allDay, String category) {
        this.title = title;
        this.description = description;
        this.startAt = startAt;
        this.durationMinutes = durationMinutes;
        this.allDay = allDay;
        this.category = category;
    }
}
