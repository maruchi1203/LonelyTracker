package com.lonelytracker.backend.user.entity;

import com.lonelytracker.backend.common.FieldLengths;
import com.lonelytracker.backend.common.security.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
 * 인증이 없어 자격 정보는 두지 않는다. 테이블명이 app_user인 것은 user가 SQL 예약어이기 때문이다.
 */
@Entity
@Table(name = "app_user")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 로그인 아이디. 전체에서 유일하다
     */
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

    /**
     * OpenAI API 키. DB에는 암호화되어 저장되고 어떤 응답에도 실리지 않는다
     */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "openai_api_key", length = 500)
    private String openAiApiKey;

    /**
     * 2분 행동 칸을 폼에 띄울지
     * 습관 도구라 기본은 켬이다
     */
    @Builder.Default
    @Column(name = "two_minute_rule", nullable = false)
    private boolean twoMinuteRule = true;

    public boolean hasOpenAiApiKey() {
        return openAiApiKey != null && !openAiApiKey.isBlank();
    }

    /**
     * OpenAI API 키를 바꾼다.
     *
     * @param openAiApiKey null이나 빈 값을 주면 등록을 해제한다
     */
    public void changeOpenAiApiKey(String apiKey) {
        this.openAiApiKey = (apiKey == null || apiKey.isBlank()) ? null : apiKey.strip();
    }

    public void changeTwoMinuteRule(boolean twoMinuteRule) {
        this.twoMinuteRule = twoMinuteRule;
    }

    public void changeDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
