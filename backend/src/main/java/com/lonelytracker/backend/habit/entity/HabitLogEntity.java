package com.lonelytracker.backend.habit.entity;

import com.lonelytracker.backend.common.FieldLengths;
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

/**
 * 습관을 해낸 하루.
 * 줄이 있으면 그날 해낸 것이다. 안 한 날은 줄을 만들지 않는다.
 */
@Entity
@Table(name = "habit_log", uniqueConstraints = @UniqueConstraint(name = "uq_habit_log_day", columnNames = {
        "habit_id", "on_date" }), indexes = @Index(name = "idx_habit_log_on_date", columnList = "on_date"))
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class HabitLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "habit_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private HabitEntity habit;

    @Column(name = "on_date", nullable = false)
    private LocalDate onDate;

    /** 그날 남긴 한 줄. 없어도 된다 */
    @Column(length = FieldLengths.TWO_MINUTE_ACTION)
    private String note;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void changeNote(String note) {
        this.note = note;
    }
}
