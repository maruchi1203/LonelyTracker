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

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.lonelytracker.backend.schedule.domain.ScheduleStatus;

/**
 * 회차별 수행 - <b>어떻게 됐나</b>.
 * <p>
 * 손댄 회차만 행이 생긴다. 행이 없다는 것은 "아직 아무것도 안 했다"(PLANNED)는 뜻이다.
 * <p>
 * {@code onDate} 는 규칙이 만들어낸 원래 날짜이고 <b>연기해도 바뀌지 않는다</b>.
 * 미루면 {@code startAt} 만 옮겨가므로, 한 행이 "계획했던 날" 과 "실제로 간 날" 을
 * 동시에 갖는다. 그래서 별도의 POSTPONED 상태나 연기 체인이 필요 없다.
 * <p>
 * 단일 일정도 "1회짜리 일정" 이므로 여기에 회차 하나를 갖는다.
 */
@Entity
@Table(name = "schedule_progress",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_progress_occurrence", columnNames = {"schedule_id", "on_date"}),
        indexes = {
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
     * FK 에 {@code ON DELETE CASCADE} 가 걸려 있다는 사실을 매핑에도 남긴다.
     * 다만 <b>런타임 동작은 바뀌지 않는다</b> — 이 어노테이션은 DDL 생성용이고
     * 이 프로젝트는 {@code ddl-auto: validate} 다. 삭제는 서비스가 명시적으로 한다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ScheduleEntity schedule;

    @Column(name = "on_date", nullable = false)
    private LocalDate onDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ScheduleStatus status = ScheduleStatus.PLANNED;

    /** 몇 번 미뤘는지. 코칭이 쓰는 지표다 */
    @Column(name = "postpone_count", nullable = false)
    @Builder.Default
    private int postponeCount = 0;

    // --- null 이면 schedule 값을 쓴다 ---

    @Column(length = FieldLengths.TITLE)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(length = FieldLengths.CATEGORY_NAME)
    private String category;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void changeStatus(ScheduleStatus status) {
        this.status = status;
    }

    /**
     * 다른 시각으로 미룬다. onDate 는 건드리지 않는다.
     *
     * @param fallbackLength 이 회차에 개별 종료 시각이 없을 때 쓸 기본 길이.
     *                       null 이면 종료 시각 없음
     */
    public void postponeTo(LocalDateTime to, Duration fallbackLength) {
        // 개별 수정으로 길이를 바꿔뒀다면 그 길이를 유지한다.
        // 무조건 기본 길이로 덮으면 사용자가 지정한 값이 조용히 사라진다.
        Duration length = (this.startAt != null && this.endAt != null)
                ? Duration.between(this.startAt, this.endAt)
                : fallbackLength;

        this.startAt = to;
        this.endAt = (length == null) ? null : to.plus(length);
        this.postponeCount++;
    }

    /** 이 회차만 수정. null 을 준 필드는 schedule 값으로 되돌아간다. */
    public void overrideFields(String title, String description, LocalDateTime startAt,
                               LocalDateTime endAt, String category) {
        this.title = title;
        this.description = description;
        this.startAt = startAt;
        this.endAt = endAt;
        this.category = category;
    }
}
