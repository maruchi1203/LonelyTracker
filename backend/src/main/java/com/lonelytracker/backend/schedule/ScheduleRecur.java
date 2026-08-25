package com.lonelytracker.backend.schedule;

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

/**
 * 반복 규칙 - <b>언제 반복되나</b>.
 * <p>
 * {@link Schedule} 과 1:1 이고 <b>이 행의 존재 자체가 "반복 여부"</b> 다.
 * 별도 플래그를 두지 않는 이유는, 플래그는 true 인데 규칙이 없는 상태가
 * 생길 수 있기 때문이다.
 * <p>
 * 시각·제목·분류는 갖지 않는다. 그건 Schedule 의 것이고 회차가 물려받는다.
 * 여기는 "며칠마다" 만 안다.
 */
@Entity
@Table(name = "schedule_recur")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ScheduleRecur {

    /** PK 가 곧 FK 다. 별도 id 를 두면 1:1 이 깨질 수 있다. */
    @Id
    @Column(name = "schedule_id")
    private Long scheduleId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Schedule schedule;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RecurrenceFreq freq;

    /** WEEKLY 일 때만 의미가 있다. DAILY 면 비어 있다. */
    @Convert(converter = WeekdaysConverter.class)
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
    public void updateRule(RecurrenceFreq freq, Set<DayOfWeek> byWeekday, LocalDate endsOn) {
        this.freq = freq;
        this.byWeekday = byWeekday;
        this.endsOn = endsOn;
    }

    /** 그만두기. 그날 회차는 남기므로 종료일에 그날을 넣는다. */
    public void stopOn(LocalDate lastDay) {
        this.endsOn = lastDay;
    }
}
