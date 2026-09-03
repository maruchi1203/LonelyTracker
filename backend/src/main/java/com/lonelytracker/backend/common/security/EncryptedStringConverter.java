package com.lonelytracker.backend.common.security;

import com.lonelytracker.backend.common.AppProperties;
import com.lonelytracker.backend.common.exception.DecryptionFailedException;
import com.lonelytracker.backend.common.exception.EncryptionNotConfiguredException;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * 민감한 문자열을 DB에 암호화해 저장한다. 지금은 사용자의 OpenAI API 키에 쓴다.
 * AES-GCM으로 암호화와 무결성 검증을 함께 하고, 저장 형식은 {@code base64(iv || ciphertext)} 다.
 * IV는 값마다 새로 만들어 같은 키를 두 번 저장해도 다른 문자열이 남는다.
 */
@Component
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;

    public EncryptedStringConverter(AppProperties properties) {
        String secret = properties.security().encryptionKey();
        // 설정이 없으면 앱은 뜨되 암호화가 필요한 기능만 막힌다
        this.key = (secret == null || secret.isBlank()) ? null : toAesKey(secret);
    }

    @Override
    public String convertToDatabaseColumn(String plain) {
        if (plain == null || plain.isBlank()) {
            return null;
        }
        requireKey();
        try {
            byte[] iv = new byte[IV_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            // 예외 메시지에 평문이 실리지 않도록 원본을 넘기지 않는다
            throw new IllegalStateException("값을 암호화하지 못했습니다");
        }
    }

    @Override
    public String convertToEntityAttribute(String stored) {
        if (stored == null || stored.isBlank()) {
            return null;
        }
        requireKey();
        try {
            byte[] combined = Base64.getDecoder().decode(stored);
            byte[] iv = Arrays.copyOfRange(combined, 0, IV_BYTES);
            byte[] cipherText = Arrays.copyOfRange(combined, IV_BYTES, combined.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new DecryptionFailedException(
                    "저장된 값을 복호화하지 못했습니다. 암호화 키가 바뀌었을 수 있습니다. "
                            + "API 키를 다시 등록해 주세요");
        }
    }

    private void requireKey() {
        if (key == null) {
            throw new EncryptionNotConfiguredException(
                    "서버에 암호화 키가 설정되지 않아 저장할 수 없습니다. "
                            + "LONELYTRACKER_ENCRYPTION_KEY 를 지정하고 다시 시작하세요");
        }
    }

    /** 설정값 길이가 제각각이라 SHA-256으로 256비트 키를 만든다 */
    private static SecretKeySpec toAesKey(String secret) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(secret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(digest, "AES");
        } catch (Exception e) {
            throw new IllegalStateException("암호화 키를 만들지 못했습니다", e);
        }
    }
}
