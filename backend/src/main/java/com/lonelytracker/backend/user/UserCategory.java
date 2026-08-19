package com.lonelytracker.backend.user;

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
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 사용자가 고를 수 있는 카테고리 목록의 한 항목.
 * <p>
 * 계층은 없다. 일정에는 이 이름이 <b>문자열로</b> 기록되므로 FK로 묶이지 않는다.
 * 목록에서 이름을 바꿔도 이미 기록된 일정의 분류는 그대로 남는다.
 * <p>
 * 이름은 한 사용자 안에서만 유일하면 된다. 사용자가 다르면 같은 이름을 쓸 수 있다.
 */
@Entity
@Table(
        name = "user_category",
        uniqueConstraints = @UniqueConstraint(name = "uq_user_category_name",
                columnNames = {"user_id", "name"}),
        indexes = @Index(name = "idx_user_category_user_id", columnList = "user_id")
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class UserCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = FieldLengths.CATEGORY_NAME)
    private String name;

    /** 화면 표시용 색상. {@code #RRGGBB} 형식을 기대하지만 강제하지는 않는다. */
    @Column(length = FieldLengths.COLOR)
    private String color;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    /** 보관 처리. 목록에서 감추되 기존 일정의 분류 문자열은 그대로 둔다. */
    @Column(nullable = false)
    @Builder.Default
    private boolean archived = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void rename(String name) {
        this.name = name;
    }

    /** null 은 "변경 없음"을 뜻한다. */
    public void updateAppearance(String color, Integer displayOrder, Boolean archived) {
        if (color != null) this.color = color;
        if (displayOrder != null) this.displayOrder = displayOrder;
        if (archived != null) this.archived = archived;
    }
}
