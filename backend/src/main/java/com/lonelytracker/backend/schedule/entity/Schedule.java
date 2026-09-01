package com.lonelytracker.backend.schedule.entity;

import com.lonelytracker.backend.common.FieldLengths;
import com.lonelytracker.backend.user.entity.User;
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
 * 일정의 정체 - <b>무엇인가</b>.
 * <p>
 * 단일 일정이든 반복 일정이든 여기 한 행이다. 반복 여부는
 * {@link ScheduleRecur} 행의 존재로 표현하고, 수행 상태는
 * {@link ScheduleProgress} 가 회차별로 갖는다.
 * <p>
 * 이 엔티티에는 <b>상태가 없다.</b> "무엇을 하기로 했나" 만 안다.
 */
@Entity
@Table(name = "schedule", indexes = {
        @Index(name = "idx_schedule_start_at", columnList = "start_at"),
        @Index(name = "idx_schedule_category", columnList = "category"),
        @Index(name = "idx_schedule_user_id", columnList = "user_id")
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

    @Column(nullable = false, length = FieldLengths.TITLE)
    private String title;

    /** 마크다운 원문을 그대로 보관한다. 렌더링은 화면이 맡는다. */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 단일 일정이면 그 시각, 반복이면 <b>첫 회차의 시각</b>이다.
     * 반복의 시작일과 시각을 이 하나로 표현한다.
     */
    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    /**
     * 소요시간(분). 종료 시각을 절대값이 아니라 <b>길이</b>로 저장한다.
     * <p>
     * 반복 회차는 날짜가 매번 다르므로 절대 시각은 첫 회차에만 쓸모가 있다.
     * 각 회차의 종료는 {@code 그 회차의 startAt + durationMinutes} 로 계산한다.
     * null 이면 종료 시각이 없는 일정이다.
     */
    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "all_day", nullable = false)
    private boolean allDay;

    /**
     * 소유자. 인증이 붙기 전까지는 기본 사용자가 들어간다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 분류. 사용자의 카테고리 목록과 FK로 묶지 않고 <b>이름을 문자열로</b> 기록한다.
     * 목록에서 이름을 바꾸거나 지워도 이미 남긴 기록은 그대로 보존된다.
     */
    @Column(length = FieldLengths.CATEGORY_NAME)
    private String category;

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
                       Integer durationMinutes, boolean allDay, String category) {
        this.title = title;
        this.description = description;
        this.startAt = startAt;
        this.durationMinutes = durationMinutes;
        this.allDay = allDay;
        this.category = category;
    }
}
