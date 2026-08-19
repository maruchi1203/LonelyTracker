package com.lonelytracker.backend.user;

import com.lonelytracker.backend.common.FieldLengths;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
 * 일정과 카테고리 목록의 소유자.
 * <p>
 * 아직 인증이 없어 비밀번호 같은 자격 정보는 두지 않는다.
 * 지금은 소유 구조만 잡아두고, 로그인은 별도 과제로 남긴다.
 * <p>
 * 테이블명이 app_user 인 이유는 user 가 SQL 예약어이기 때문이다.
 */
@Entity
@Table(name = "app_user")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 로그인 아이디 역할. 전체에서 유일하다. */
    @Column(nullable = false, length = FieldLengths.USERNAME, unique = true)
    private String username;

    @Column(name = "display_name", length = FieldLengths.DISPLAY_NAME)
    private String displayName;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void changeDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
