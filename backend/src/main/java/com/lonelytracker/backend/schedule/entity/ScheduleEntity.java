package com.lonelytracker.backend.schedule.entity;

import com.lonelytracker.backend.common.FieldLengths;
import com.lonelytracker.backend.user.entity.UserEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 단일·반복 일정의 공통 정보.
 * 반복 규칙은 {@link ScheduleRecurEntity},
 * 회차별 수행은 {@link ScheduleProgressEntity}에 있다.
 */
@Entity
@Table(name = "schedule", indexes = {
        @Index(name = "idx_schedule_start_at", columnList = "start_at"),
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
     * 없으면 아직 언제 할지 안 정한 항목이라 회차가 생기지 않는다
     */
    @Column(name = "start_at")
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
     * 태그
     * 일정 단위라 회차마다 달라지지 않는다
     */
    @ElementCollection
    @CollectionTable(name = "schedule_tag", joinColumns = @JoinColumn(name = "schedule_id"))
    @Column(name = "name", length = FieldLengths.TAG)
    @Builder.Default
    private Set<String> tags = new HashSet<>();

    /**
     * 태그 사본
     * 지연 로딩 컬렉션을 그대로 내보내면 세션이 닫힌 뒤 직렬화가 깨진다
     */
    public Set<String> tagsCopy() {
        return Set.copyOf(tags);
    }

    /**
     * 수행 장소
     */
    @Column(length = FieldLengths.PLACE)
    private String place;

    /**
     * 2분 행동
     * 시작에 필요한 2분 이내의 미니 행동이다
     */
    @Column(name = "two_minute_action", length = FieldLengths.TWO_MINUTE_ACTION)
    private String twoMinuteAction;

    /**
     * 1회성 일정의 완료 시각
     * 값이 있으면 완료다. 습관은 회차마다 {@link ScheduleProgressEntity} 가 맡는다
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * 상위 일정
     * 연관이 아니라 id로 둔다. 응답이 id만 쓰고, 지연 로딩 사고를 만들지 않는다
     * 부모가 지워지면 DB가 NULL로 되돌려 자식은 최상위가 된다
     */
    @Column(name = "parent_id")
    private Long parentId;

    /**
     * 기한
     * 언제까지 해내야 하는가. 언제 시작하는가({@code startAt})와는 다르다
     */
    @Column(name = "due_on")
    private LocalDate dueOn;

    /**
     * 형제 사이의 순서
     * 최상위끼리는 부모가 없는 한 무리로 본다
     */
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

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
     * @param tags            태그
     * @param place           수행 장소
     * @param twoMinuteAction 2분 행동
     * @param parentId        상위 일정
     * @param dueOn           기한
     */
    /**
     * 완료 여부를 바꾼다.
     *
     * @param completed 풀면 완료 시각이 지워진다
     */
    /**
     * 상위를 바꾼다
     *
     * @param parentId null이면 최상위가 된다
     */
    public void changeParent(Long parentId) {
        this.parentId = parentId;
    }

    public void changeCompletion(boolean completed) {
        this.completedAt = completed ? LocalDateTime.now() : null;
    }

    public void update(String title, String description, LocalDateTime startAt,
            Integer durationMinutes, boolean allDay, Set<String> tags,
            String place, String twoMinuteAction, Long parentId, LocalDate dueOn) {
        this.title = title;
        this.description = description;
        this.startAt = startAt;
        this.durationMinutes = durationMinutes;
        this.allDay = allDay;
        this.tags = (tags == null) ? new HashSet<>() : new HashSet<>(tags);
        this.place = place;
        this.twoMinuteAction = twoMinuteAction;
        this.parentId = parentId;
        this.dueOn = dueOn;
    }
}
