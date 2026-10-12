package com.lonelytracker.backend.habit.entity;

import com.lonelytracker.backend.common.FieldLengths;
import com.lonelytracker.backend.common.Orderable;
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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 기르는 습관 하나.
 * 일정과 섞지 않는다. 탭을 붙였다 뗄 수 있어야 해서 테이블을 따로 둔다.
 */
@Entity
@Table(name = "habit", indexes = @Index(name = "idx_habit_user_id", columnList = "user_id"))
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class HabitEntity implements Orderable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserEntity user;

    @Column(nullable = false, length = FieldLengths.TITLE)
    private String title;

    /**
     * 기르는 카테고리. 화면이 카테고리마다 묶어 보여준다.
     * 카테고리가 지워지면 이 습관도 함께 간다 — 카테고리 없는 습관은 둘 자리가 없다
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private HabitCategoryEntity category;

    /** 시작에 필요한 2분 이내의 행동. 습관마다 하나다 */
    @Column(name = "two_minute_action", length = FieldLengths.TWO_MINUTE_ACTION)
    private String twoMinuteAction;

    /** 언제 할지. 시각일 수도, "퇴근 후" 같은 상황일 수도 있다 */
    @Column(name = "at_time", length = FieldLengths.HABIT_AT_TIME)
    private String atTime;

    /** 어디서 할지 */
    @Column(length = FieldLengths.PLACE)
    private String place;

    /** 같은 카테고리 안에서의 자리 */
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    /** 그만둔 시각. 값이 있으면 목록에서 내려가되 지난 기록은 남는다 */
    @Column(name = "archived_at")
    private LocalDateTime archivedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void update(String title, HabitCategoryEntity category, String twoMinuteAction,
            String atTime, String place) {
        this.title = title;
        this.category = category;
        this.twoMinuteAction = twoMinuteAction;
        this.atTime = atTime;
        this.place = place;
    }

    public void changeDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    /**
     * 그만두거나 다시 시작한다
     *
     * @param archived 풀면 그만둔 시각이 지워진다
     */
    public void changeArchived(boolean archived) {
        this.archivedAt = archived ? LocalDateTime.now() : null;
    }

    public boolean isArchived() {
        return archivedAt != null;
    }
}
