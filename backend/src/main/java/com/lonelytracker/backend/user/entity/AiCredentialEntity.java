package com.lonelytracker.backend.user.entity;

import com.lonelytracker.backend.common.FieldLengths;
import com.lonelytracker.backend.common.security.EncryptedStringConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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

import java.time.LocalDateTime;

/**
 * AI 제공자 하나에 대한 자격 증명.
 * 제공자는 주소로 갈려서 한 사용자에게 주소마다 한 줄이다.
 */
@Entity
@Table(name = "ai_credential", uniqueConstraints = @UniqueConstraint(name = "uq_ai_credential_base_url", columnNames = {
        "user_id", "base_url" }))
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class AiCredentialEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserEntity user;

    @Column(name = "base_url", nullable = false, length = FieldLengths.AI_BASE_URL)
    private String baseUrl;

    @Column(nullable = false, length = FieldLengths.AI_MODEL)
    private String model;

    /**
     * 암호화되어 저장되고 어떤 응답에도 실리지 않는다
     */
    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "api_key", nullable = false, length = 500)
    private String apiKey;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void changeModel(String model) {
        this.model = model;
    }

    public void changeApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
