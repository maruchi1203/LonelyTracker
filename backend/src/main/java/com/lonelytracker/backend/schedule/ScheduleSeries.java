package com.lonelytracker.backend.schedule;

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
import java.util.Set;

/**
 * 반복 규칙. <b>규칙만</b> 담고 제목·설명·분류·시각은 담지 않는다.
 * <p>
 * 넣으면 같은 정보가 시리즈와 각 회차에 이중으로 존재하고, 한쪽만 수정됐을 때
 * 어느 쪽이 진실인지 알 수 없다. 회차 행이 진실이고 시리즈는
 * "어떤 간격으로 만들어졌는가" 만 안다.
 */
@Entity
@Table(name = "schedule_series")
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RecurrenceFreq freq;

    /** WEEKLY 일 때만 의미가 있다. DAILY 면 비어 있다. */
    @Convert(converter = WeekdaysConverter.class)
    @Column(name = "by_weekday", length = 30)
    private Set<DayOfWeek> byWeekday;

    @Column(name = "starts_on", nullable = false)
    private LocalDate startsOn;

    /** null 이면 무기한. 생성은 1년치까지만 한다. */
    @Column(name = "ends_on")
    private LocalDate endsOn;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 규칙이 바뀌면 미래 회차를 지우고 다시 만들어야 한다. 호출자가 그 순서를 지킨다. */
    public void updateRule(RecurrenceFreq freq, Set<DayOfWeek> byWeekday,
                           LocalDate startsOn, LocalDate endsOn) {
        this.freq = freq;
        this.byWeekday = byWeekday;
        this.startsOn = startsOn;
        this.endsOn = endsOn;
    }
}
