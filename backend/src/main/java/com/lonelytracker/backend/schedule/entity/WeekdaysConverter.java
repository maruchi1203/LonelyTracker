package com.lonelytracker.backend.schedule.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@code Set<DayOfWeek>} 를 {@code "MON,WED,FRI"} 로 저장한다.
 * <p>
 * 엔티티 쪽은 타입 안전하게 쓰고 저장만 문자열인 모양이다.
 * 별도 테이블로 정규화하지 않는 이유는 {@code schedule.category} 를 문자열로 둔 것과 같다.
 */
@Converter
public class WeekdaysConverter implements AttributeConverter<Set<DayOfWeek>, String> {

    /** DayOfWeek 이름 앞 세 글자. MON/TUE/WED/THU/FRI/SAT/SUN 으로 전부 다르다. */
    private static final int ABBREVIATION_LENGTH = 3;

    @Override
    public String convertToDatabaseColumn(Set<DayOfWeek> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        // 정렬해야 같은 집합이 항상 같은 문자열이 된다. 안 그러면 불필요한 UPDATE 가 생긴다.
        return attribute.stream()
                .sorted()
                .map(day -> day.name().substring(0, ABBREVIATION_LENGTH))
                .collect(Collectors.joining(","));
    }

    @Override
    public Set<DayOfWeek> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return EnumSet.noneOf(DayOfWeek.class);
        }
        return Arrays.stream(dbData.split(","))
                .map(String::strip)
                .map(WeekdaysConverter::toDayOfWeek)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));
    }

    private static DayOfWeek toDayOfWeek(String abbreviation) {
        for (DayOfWeek day : DayOfWeek.values()) {
            if (day.name().startsWith(abbreviation)) {
                return day;
            }
        }
        throw new IllegalArgumentException("알 수 없는 요일 표기: " + abbreviation);
    }
}
