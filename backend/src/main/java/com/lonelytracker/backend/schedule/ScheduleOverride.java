package com.lonelytracker.backend.schedule;

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

/**
 * 손댄 회차 하나. 안 건드린 회차는 행이 없다.
 * <p>
 * {@code onDate} 는 규칙이 만들어낸 원래 날짜이고 <b>연기해도 바뀌지 않는다</b>.
 * 미루면 {@code startAt} 만 옮겨가므로, 한 행이 "계획했던 날" 과 "실제로 간 날" 을
 * 동시에 갖는다. 그래서 별도의 POSTPONED 상태나 연기 체인이 필요 없다.
 */
@Entity
@Table(name = "schedule_override",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_override_occurrence", columnNames = {"series_id", "on_date"}),
        indexes = {
                @Index(name = "idx_override_on_date", columnList = "on_date"),
                @Index(name = "idx_override_start_at", columnList = "start_at")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ScheduleOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * FK 에 {@code ON DELETE CASCADE} 가 걸려 있다는 사실을 매핑에도 남긴다.
     * <p>
     * <b>런타임 동작은 바뀌지 않는다.</b> 이 어노테이션은 DDL 생성용이고 이 프로젝트는
     * {@code ddl-auto: validate} 라 Hibernate 가 DDL 을 만들지 않는다. 게다가
     * {@code cascade=REMOVE} 없이 단독으로 쓰면 연관 엔티티가 <b>영속성 컨텍스트에서
     * 삭제 표시되지 않는다</b>.
     * <p>
     * 그래서 시리즈 전체 삭제는 {@code ScheduleOverrideRepository.deleteBySeriesId} 로
     * 명시적으로 먼저 지운다. 그러지 않으면 flush 때
     * {@code TransientPropertyValueException} 이 난다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ScheduleSeries series;

    @Column(name = "on_date", nullable = false)
    private LocalDate onDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ScheduleStatus status = ScheduleStatus.PLANNED;

    @Column(name = "postpone_count", nullable = false)
    @Builder.Default
    private int postponeCount = 0;

    // --- null 이면 시리즈 값을 쓴다 ---

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
     * @param length 원래 소요시간. null 이면 종료 시각 없음
     */
    public void postponeTo(LocalDateTime to, Duration length) {
        this.startAt = to;
        this.endAt = (length == null) ? null : to.plus(length);
        this.postponeCount++;
    }

    /** 이 회차만 수정. null 을 주면 시리즈 값으로 되돌아간다. */
    public void overrideFields(String title, String description, LocalDateTime startAt,
                               LocalDateTime endAt, String category) {
        this.title = title;
        this.description = description;
        this.startAt = startAt;
        this.endAt = endAt;
        this.category = category;
    }
}
