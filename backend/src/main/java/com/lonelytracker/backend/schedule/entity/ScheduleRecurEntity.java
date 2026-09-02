package com.lonelytracker.backend.schedule.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import com.lonelytracker.backend.schedule.domain.ScheduleRecurrenceFreq;

/**
 * 반복 규칙
 */
@Entity
@Table(name = "schedule_recur")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ScheduleRecurEntity {
    /** PK가 곧 FK다. 별도 id 를 두면 1:1 이 깨질 수 있다. */
    @Id
    @Column(name = "schedule_id")
    private Long scheduleId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ScheduleEntity schedule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ScheduleRecurrenceFreq freq;

    /** WEEKLY일 때만 의미가 있다. DAILY 면 비어 있다. */
    @Convert(converter = ScheduleWeekdaysConverter.class)
    @Column(name = "by_weekday", length = 30)
    private Set<DayOfWeek> byWeekday;

    /** null 이면 무기한. 회차를 미리 만들지 않으므로 상한이 필요 없다. */
    @Column(name = "ends_on")
    private LocalDate endsOn;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 규칙 변경. 전개가 조회 시점이므로 회차를 다시 만들 필요가 없다. */
    public void updateRule(ScheduleRecurrenceFreq freq, Set<DayOfWeek> byWeekday, LocalDate endsOn) {
        this.freq = freq;
        this.byWeekday = byWeekday;
        this.endsOn = endsOn;
    }

    /** 그만두기. 그날 회차는 남기므로 종료일에 그날을 넣는다. */
    public void stopOn(LocalDate lastDay) {
        this.endsOn = lastDay;
    }
}
