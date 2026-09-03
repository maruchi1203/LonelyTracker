package com.lonelytracker.backend.schedule.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 요일 집합을 {@code "MON, WED, FRI"} 문자열로 저장한다.
 */
@Converter
public class ScheduleWeekdaysConverter implements AttributeConverter<Set<DayOfWeek>, String> {

    /** 요일 표기 길이. 앞 세 글자면 일곱 요일이 전부 구분된다 */
    private static final int ABBREVIATION_LENGTH = 3;

    @Override
    public String convertToDatabaseColumn(Set<DayOfWeek> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        // 정렬해야 같은 집합이 항상 같은 문자열이 된다
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
                .map(ScheduleWeekdaysConverter::toDayOfWeek)
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
