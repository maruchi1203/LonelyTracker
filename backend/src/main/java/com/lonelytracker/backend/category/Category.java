package com.lonelytracker.backend.category;

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
 * 일정 분류. 계층은 {@link #parent}로 표현하고, 조회 편의를 위해
 * 전체 경로를 {@link #path}에 함께 들고 간다.
 * <p>
 * path를 중복 보관하는 이유는 "하위 포함 필터"를 재귀 조회 없이
 * prefix 비교 한 번으로 끝내기 위함이다. 대신 이름이 바뀌면
 * 자신과 후손의 path를 함께 갱신해야 한다 ({@code CategoryService.rename}).
 */
@Entity
@Table(name = "category", indexes = {
        @Index(name = "idx_category_path", columnList = "path"),
        @Index(name = "idx_category_parent_id", columnList = "parent_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 마지막 세그먼트만 담는다. 예: {@code 능력\개발} 의 name 은 {@code 개발} */
    @Column(nullable = false, length = 50)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Category parent;

    /** 루트부터의 전체 경로. 유일해야 한다. */
    @Column(nullable = false, length = 255, unique = true)
    private String path;

    /** 같은 부모 안에서의 정렬 순서 */
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    /** 화면 표시용 색상. {@code #RRGGBB} 형식을 기대하지만 강제하지는 않는다. */
    @Column(length = 20)
    private String color;

    /** 사이드바에서 접혀 있는지 */
    @Column(nullable = false)
    @Builder.Default
    private boolean collapsed = false;

    /** 보관 처리. 목록에서 감추되 기존 일정의 분류는 유지한다. */
    @Column(nullable = false)
    @Builder.Default
    private boolean archived = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 이름과 경로는 항상 함께 바뀐다. 따로 바꾸면 계층이 깨진다. */
    void rename(String name, String path) {
        this.name = name;
        this.path = path;
    }

    void updateAppearance(String color, Integer displayOrder, Boolean collapsed, Boolean archived) {
        if (color != null) this.color = color;
        if (displayOrder != null) this.displayOrder = displayOrder;
        if (collapsed != null) this.collapsed = collapsed;
        if (archived != null) this.archived = archived;
    }
}
