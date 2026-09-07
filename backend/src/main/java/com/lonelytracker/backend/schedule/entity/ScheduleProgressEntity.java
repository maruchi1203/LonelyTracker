package com.lonelytracker.backend.schedule.entity;

import com.lonelytracker.backend.common.FieldLengths;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.lonelytracker.backend.schedule.domain.ScheduleStatus;

/**
 * ScheduleEntity
 */
@Entity
@Table(name = "schedule_progress", uniqueConstraints = @UniqueConstraint(name = "uq_progress_occurrence", columnNames = {
                "schedule_id", "on_date" }), indexes = {
                                @Index(name = "idx_progress_on_date", columnList = "on_date"),
                                @Index(name = "idx_progress_start_at", columnList = "start_at"),
                                @Index(name = "idx_progress_status", columnList = "status")
                })
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ScheduleProgressEntity {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        /**
         * 단일 혹은 반복 스케줄
         * OnDelete는 Cascade가 적용된 Column임을 표시하기 위한 Annotation
         * 실질적 기능 없음
         */
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "schedule_id", nullable = false)
        @OnDelete(action = OnDeleteAction.CASCADE)
        private ScheduleEntity schedule;

        /**
         * 일정 날짜 (일정이 2일 이상일 시 구별 단위)
         */
        @Column(name = "on_date", nullable = false)
        private LocalDate onDate;

        /**
         * 일정 상태 {@link ScheduleStatus}
         */
        @Enumerated(EnumType.STRING)
        @Column(nullable = false, length = 20)
        @Builder.Default
        private ScheduleStatus status = ScheduleStatus.PLANNED;

        /**
         * 제목
         */
        @Column(length = FieldLengths.TITLE)
        private String title;

        /**
         * 설명
         */
        @Column(columnDefinition = "TEXT")
        private String description;

        /**
         * 시작일시
         */
        @Column(name = "start_at")
        private LocalDateTime startAt;

        /**
         * 종료일시
         */
        @Column(name = "end_at")
        private LocalDateTime endAt;

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
         * 일정 상태를 수정
         * 
         * @param status 일정 상태 {@link ScheduleStatus}
         */
        public void changeStatus(ScheduleStatus status) {
                this.status = status;
        }

        /**
         * 일정 정보을 변경한다.
         * 
         * @param title       제목
         * @param description 설명
         * @param startAt     시작일시
         * @param endAt       종료일시
         */
        public void overrideFields(String title, String description, LocalDateTime startAt,
                        LocalDateTime endAt) {
                this.title = title;
                this.description = description;
                this.startAt = startAt;
                this.endAt = endAt;
        }
}
