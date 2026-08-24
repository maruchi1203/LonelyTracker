package com.lonelytracker.backend.schedule;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 요일 집합을 문자열로 저장하는 컨버터.
 * 정규화 대신 문자열을 쓰는 이유는 요일이 최대 7개로 고정이고 따로 조회할 일이 없기 때문이다.
 * DB 를 눈으로 열었을 때 읽히는 것도 이점이다.
 */
class WeekdaysConverterTest {

    private final WeekdaysConverter converter = new WeekdaysConverter();

    @Test
    @DisplayName("요일 집합은 항상 같은 순서의 문자열이 된다")
    void writesInStableOrder() {
        Set<DayOfWeek> input = EnumSet.of(DayOfWeek.FRIDAY, DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY);

        assertThat(converter.convertToDatabaseColumn(input)).isEqualTo("MON,WED,FRI");
    }

    @Test
    @DisplayName("문자열을 다시 읽으면 원래 집합이 된다")
    void roundTrips() {
        Set<DayOfWeek> input = EnumSet.of(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY);

        String stored = converter.convertToDatabaseColumn(input);

        assertThat(converter.convertToEntityAttribute(stored)).isEqualTo(input);
    }

    @Test
    @DisplayName("일곱 요일을 모두 담아도 컬럼 길이 30을 넘지 않는다")
    void fitsInColumn() {
        String stored = converter.convertToDatabaseColumn(EnumSet.allOf(DayOfWeek.class));

        assertThat(stored).hasSizeLessThanOrEqualTo(30);
    }

    @Test
    @DisplayName("null 과 빈 집합은 null 로 저장된다")
    void emptyBecomesNull() {
        assertThat(converter.convertToDatabaseColumn(null)).isNull();
        assertThat(converter.convertToDatabaseColumn(EnumSet.noneOf(DayOfWeek.class))).isNull();
    }

    @Test
    @DisplayName("null 을 읽으면 빈 집합이 된다")
    void nullBecomesEmpty() {
        assertThat(converter.convertToEntityAttribute(null)).isEmpty();
    }

    @Test
    @DisplayName("알 수 없는 표기는 예외를 던진다")
    void rejectsUnknownAbbreviation() {
        assertThatThrownBy(() -> converter.convertToEntityAttribute("MON,XXX"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
