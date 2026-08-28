package com.lonelytracker.backend.common.security;

import com.lonelytracker.backend.common.AppProperties;
import com.lonelytracker.backend.common.exception.DecryptionFailedException;
import com.lonelytracker.backend.common.exception.EncryptionNotConfiguredException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 민감한 값의 암호화. DB 를 타지 않는 순수 계산이라 단위 테스트로 둔다.
 */
class EncryptedStringConverterTest {

    private static final String SECRET = "sk-proj-verysecretvalue1234";

    private final EncryptedStringConverter converter = converterWith("test-master-key");

    @Test
    @DisplayName("암호문에는 평문이 남지 않는다")
    void cipherTextHidesPlainText() {
        String stored = converter.convertToDatabaseColumn(SECRET);

        assertThat(stored).isNotNull();
        assertThat(stored).doesNotContain(SECRET);
    }

    @Test
    @DisplayName("암호화한 값을 다시 읽으면 원본이 된다")
    void roundTrips() {
        String stored = converter.convertToDatabaseColumn(SECRET);

        assertThat(converter.convertToEntityAttribute(stored)).isEqualTo(SECRET);
    }

    @Test
    @DisplayName("같은 값을 두 번 암호화해도 결과가 다르다")
    void usesFreshIvEachTime() {
        // IV 를 값마다 새로 만들지 않으면 같은 평문이 항상 같은 암호문이 되어
        // "이 두 사용자는 같은 키를 쓴다" 가 DB 만 보고도 드러난다
        String first = converter.convertToDatabaseColumn(SECRET);
        String second = converter.convertToDatabaseColumn(SECRET);

        assertThat(first).isNotEqualTo(second);
        assertThat(converter.convertToEntityAttribute(first)).isEqualTo(SECRET);
        assertThat(converter.convertToEntityAttribute(second)).isEqualTo(SECRET);
    }

    @Test
    @DisplayName("저장값이 조작되면 복호화가 실패한다")
    void detectsTampering() {
        // AES-GCM 은 암호화와 무결성 검증을 함께 한다.
        // 누가 DB 값을 바꿔치기하면 조용히 이상한 값이 나오는 게 아니라 예외가 난다
        String stored = converter.convertToDatabaseColumn(SECRET);
        String tampered = stored.substring(0, stored.length() - 4) + "AAAA";

        assertThatThrownBy(() -> converter.convertToEntityAttribute(tampered))
                .isInstanceOf(DecryptionFailedException.class);
    }

    @Test
    @DisplayName("다른 마스터 키로는 복호화되지 않는다")
    void otherMasterKeyCannotDecrypt() {
        String stored = converter.convertToDatabaseColumn(SECRET);

        assertThatThrownBy(() -> converterWith("다른-키").convertToEntityAttribute(stored))
                .isInstanceOf(DecryptionFailedException.class);
    }

    @Test
    @DisplayName("null과 빈 값은 그대로 null이 된다")
    void nullPassesThrough() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToDatabaseColumn("  ")).isNull();
        assertThat(converter.convertToEntityAttribute(null)).isNull();
    }

    @Test
    @DisplayName("마스터 키가 없으면 저장 시도에서 막힌다")
    void requiresMasterKey() {
        // 앱은 뜨되 암호화가 필요한 기능만 막힌다.
        // 일정 CRUD 는 키 없이도 되므로 기동을 막을 이유가 없다
        assertThatThrownBy(() -> converterWith("").convertToDatabaseColumn(SECRET))
                .isInstanceOf(EncryptionNotConfiguredException.class)
                .hasMessageContaining("암호화 키");
    }

    private EncryptedStringConverter converterWith(String masterKey) {
        return new EncryptedStringConverter(new AppProperties(
                new AppProperties.UserDefaults("default", List.of()),
                new AppProperties.AiSetting("http://localhost", "test-model",
                        Duration.ofSeconds(5), Duration.ofSeconds(30), 2),
                new AppProperties.Security(masterKey)));
    }
}
