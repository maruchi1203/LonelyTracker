package com.lonelytracker.backend.schedule;

import com.lonelytracker.backend.common.FieldLengths;
import com.lonelytracker.backend.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

/**
 * 반복 규칙 + 템플릿.
 * <p>
 * 회차를 행으로 만들지 않으므로 제목·시각·분류의 "진실" 이 여기에 있다.
 * 날짜는 규칙이 정하므로 시각만 갖는다 - startTime 과 durationMinutes.
 */
@Entity
@Table(name = "schedule_series", indexes = {
        @Index(name = "idx_series_user_id", columnList = "user_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ScheduleSeries {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // --- 규칙 ---

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RecurrenceFreq freq;

    /** WEEKLY 일 때만 의미가 있다. DAILY 면 비어 있다. */
    @Convert(converter = WeekdaysConverter.class)
    @Column(name = "by_weekday", length = 30)
    private Set<DayOfWeek> byWeekday;

    @Column(name = "starts_on", nullable = false)
    private LocalDate startsOn;

    /** null 이면 무기한. 회차를 미리 만들지 않으므로 상한이 필요 없다. */
    @Column(name = "ends_on")
    private LocalDate endsOn;

    // --- 템플릿 ---

    @Column(nullable = false, length = FieldLengths.TITLE)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    /** null 이면 종료 시각이 없는 일정이다. */
    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(name = "all_day", nullable = false)
    private boolean allDay;

    @Column(length = FieldLengths.CATEGORY_NAME)
    private String category;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 규칙 변경. 전개가 조회 시점이므로 재생성이 필요 없다. */
    public void updateRule(RecurrenceFreq freq, Set<DayOfWeek> byWeekday, LocalDate endsOn) {
        this.freq = freq;
        this.byWeekday = byWeekday;
        this.endsOn = endsOn;
    }

    /** 템플릿 변경 = "앞으로 전부 수정". override 가 있는 회차는 영향받지 않는다. */
    public void updateTemplate(String title, String description, LocalTime startTime,
                               Integer durationMinutes, boolean allDay, String category) {
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.durationMinutes = durationMinutes;
        this.allDay = allDay;
        this.category = category;
    }

    /** 그만두기. 그날 회차는 남기므로 종료일에 그날을 넣는다. */
    public void stopOn(LocalDate lastDay) {
        this.endsOn = lastDay;
    }
}
