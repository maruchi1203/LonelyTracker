package com.lonelytracker.backend.schedule;

import jakarta.persistence.Column;
import com.lonelytracker.backend.category.Category;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
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

@Entity
@Table(name = "schedule", indexes = {
        @Index(name = "idx_schedule_start_at", columnList = "start_at"),
        @Index(name = "idx_schedule_status", columnList = "status"),
        @Index(name = "idx_schedule_category_id", columnList = "category_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
// JPA는 기본 생성자를 요구하지만, 외부에서 빈 객체를 만들지 못하도록 PROTECTED로 막는다
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    /**
     * 마크다운 원문을 그대로 보관한다. 렌더링은 화면에서 담당한다.
     * 문서처럼 길어질 수 있어 varchar 대신 TEXT를 쓴다.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "all_day", nullable = false)
    private boolean allDay;

    // ORDINAL(숫자)로 지정하면 enum 순서가 바뀌는 사태가 발생할 때
    // "enum 값-enum 이름"이 불일치하는 상황이 될 수 있음
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ScheduleStatus status = ScheduleStatus.PLANNED;

    /**
     * 분류. 계층은 Category 쪽에서 관리한다.
     * LAZY라서 DTO 변환은 트랜잭션 안에서 끝내야 한다(open-in-view: false).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * setter를 열어두는 대신 의도가 드러나는 메서드로 상태를 바꾼다.
     * 영속 상태의 엔티티라면 트랜잭션 종료 시 변경 감지(dirty checking)로 자동 UPDATE된다.
     */
    public void update(String title, String description, LocalDateTime startAt,
                       LocalDateTime endAt, boolean allDay, Category category) {
        this.title = title;
        this.description = description;
        this.startAt = startAt;
        this.endAt = endAt;
        this.allDay = allDay;
        this.category = category;
    }

    public void changeStatus(ScheduleStatus status) {
        this.status = status;
    }
}
