package com.lonelytracker.backend.schedule.domain;

import com.lonelytracker.backend.schedule.dto.ScheduleRecurrenceRequest;
import com.lonelytracker.backend.schedule.entity.ScheduleEntity;
import com.lonelytracker.backend.schedule.entity.ScheduleRecurEntity;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 일정 계산에 쓰는 순수 함수 모음. DB와 스프링을 모른다.
 */
public final class ScheduleUtil {

    /** 종료일을 안 정한 규칙을 검사할 때 들여다볼 기간 */
    private static final int RULE_CHECK_MONTHS = 2;

    /** 일정 하나가 이어질 수 있는 최대 길이. 소요시간을 int 분으로 저장한다 */
    private static final Duration MAX_DURATION = Duration.ofDays(366);

    /** 한 번에 조회할 수 있는 최대 구간 */
    private static final Duration MAX_WINDOW = Duration.ofDays(366);

    private ScheduleUtil() {
    }

    /**
     * 시작~종료 사이의 분. 일정은 종료 시각 대신 이 값을 저장한다.
     *
     * @param endAt null이면 소요시간 없음
     * @return 분 단위 소요시간. endAt이 null이면 null
     */
    public static Integer toMinutes(LocalDateTime startAt, LocalDateTime endAt) {
        return (endAt == null) ? null : (int) Duration.between(startAt, endAt).toMinutes();
    }

    /**
     * 요일 집합을 EnumSet으로 바꾼다.
     * EnumSet.copyOf는 빈 컬렉션에 예외를 던지는데, DAILY는 요일이 비는 게 정상이다.
     *
     * @param source null이거나 비어 있어도 된다
     */
    public static Set<DayOfWeek> toWeekdaySet(Set<DayOfWeek> source) {
        Set<DayOfWeek> weekdays = EnumSet.noneOf(DayOfWeek.class);
        if (source != null) {
            weekdays.addAll(source);
        }
        return weekdays;
    }

    /**
     * 태그를 다듬는다. 앞뒤 공백을 떼고 빈 것은 버린다.
     *
     * @return 중복 없는 태그 집합. 준 것이 없으면 빈 집합
     */
    public static Set<String> normalizeTags(Set<String> tags) {
        if (tags == null) {
            return new HashSet<>();
        }
        return tags.stream()
                .filter(t -> t != null && !t.isBlank())
                .map(String::strip)
                .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * 일정 기간의 방향과 길이를 검사한다.
     *
     * @param endAt null이면 검사하지 않는다
     * @throws IllegalArgumentException 뒤집혔거나 366일을 넘을 때
     */
    public static void validatePeriod(LocalDateTime startAt, LocalDateTime endAt) {
        if (endAt == null) {
            return;
        }
        if (endAt.isBefore(startAt)) {
            throw new IllegalArgumentException("endAt은 startAt보다 이를 수 없습니다");
        }
        // 소요시간을 int 분으로 저장하므로 상한이 없으면 넘쳐서 음수가 된다
        if (Duration.between(startAt, endAt).compareTo(MAX_DURATION) > 0) {
            throw new IllegalArgumentException(
                    "일정 하나는 " + MAX_DURATION.toDays() + "일을 넘을 수 없습니다");
        }
    }

    /**
     * 조회 구간의 방향과 크기를 검사한다.
     *
     * @throws IllegalArgumentException 뒤집혔거나 366일을 넘을 때
     */
    public static void validateWindow(LocalDateTime from, LocalDateTime to) {
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("to는 from보다 이를 수 없습니다");
        }
        if (Duration.between(from, to).compareTo(MAX_WINDOW) > 0) {
            throw new IllegalArgumentException(
                    "조회 구간은 " + MAX_WINDOW.toDays() + "일을 넘을 수 없습니다");
        }
    }


    /**
     * 회차가 자기 날짜에서 너무 멀어지지 않았는지 검사한다.
     *
     * @param instanceDate 규칙이 만든 원래 날짜
     * @throws IllegalArgumentException 366일 넘게 떨어졌을 때
     */
    public static void validateInstanceStart(LocalDate instanceDate, LocalDateTime startAt) {
        Duration gap = Duration.between(instanceDate.atStartOfDay(), startAt).abs();
        if (gap.compareTo(MAX_DURATION) > 0) {
            throw new IllegalArgumentException(
                    "회차는 자기 날짜에서 " + MAX_DURATION.toDays() + "일 넘게 떨어질 수 없습니다");
        }
    }

    /**
     * 규칙이 회차를 하나라도 내는지 검사한다.
     *
     * @throws IllegalArgumentException 회차가 하나도 안 생길 때
     */
    public static void validateRule(ScheduleEntity schedule, ScheduleRecurrenceRequest rule) {
        LocalDate start = schedule.getStartAt().toLocalDate();
        Set<DayOfWeek> weekdays = toWeekdaySet(rule.byWeekday());
        if (expandForCheck(rule.freq(), weekdays, start, rule.endsOn()).isEmpty()) {
            throw new IllegalArgumentException("이 규칙으로는 일정이 하나도 생기지 않습니다");
        }

        Integer minutes = schedule.getDurationMinutes();
        if (minutes == null) {
            return;
        }
        Duration gap = gapToNextInstance(rule.freq(), weekdays);
        if (Duration.ofMinutes(minutes).compareTo(gap) > 0) {
            throw new IllegalArgumentException(
                    "반복 일정은 다음 회차가 시작하기 전에 끝나야 합니다. 최대 "
                            + gap.toHours() + "시간");
        }
    }

    /**
     * 회차 사이의 가장 짧은 간격. 반복 일정의 소요시간 상한이 된다.
     *
     * @param byWeekday WEEKLY 일 때만 본다. 매일이면 24시간이다
     */
    public static Duration gapToNextInstance(ScheduleRecurrenceFreq freq, Set<DayOfWeek> byWeekday) {
        if (freq == ScheduleRecurrenceFreq.DAILY || byWeekday.size() <= 1) {
            return (freq == ScheduleRecurrenceFreq.DAILY)
                    ? Duration.ofDays(1)
                    : Duration.ofDays(7);
        }

        List<Integer> days = byWeekday.stream().map(DayOfWeek::getValue).sorted().toList();
        int min = 7 - days.get(days.size() - 1) + days.get(0);   // 마지막 요일에서 다음 주 첫 요일까지
        for (int i = 1; i < days.size(); i++) {
            min = Math.min(min, days.get(i) - days.get(i - 1));
        }
        return Duration.ofDays(min);
    }

    /**
     * 첫 회차 날짜. 반복이 아니면 일정 자신의 날짜다.
     *
     * @param recur null이면 1회성 일정
     * @throws IllegalArgumentException 규칙으로 회차가 하나도 안 생길 때
     */
    public static LocalDate firstDateOf(ScheduleEntity schedule, ScheduleRecurEntity recur) {
        LocalDate start = schedule.getStartAt().toLocalDate();
        if (recur == null) {
            return start;
        }

        List<LocalDate> dates = expandForCheck(
                recur.getFreq(), recur.getByWeekday(), start, recur.getEndsOn());
        if (dates.isEmpty()) {
            throw new IllegalArgumentException("이 규칙으로는 일정이 하나도 생기지 않습니다");
        }
        return dates.get(0);
    }

    /**
     * 그 일정이 그 날짜에 회차를 내는지.
     *
     * @param recur null이면 1회성 일정
     */
    public static boolean occursOn(ScheduleEntity schedule, ScheduleRecurEntity recur,
            LocalDate onDate) {
        LocalDate start = schedule.getStartAt().toLocalDate();
        if (onDate.isBefore(start)) {
            return false;
        }
        if (recur == null) {
            return onDate.equals(start);
        }
        if (recur.getEndsOn() != null && onDate.isAfter(recur.getEndsOn())) {
            return false;
        }
        return !ScheduleInstanceDates.generate(
                recur.getFreq(), recur.getByWeekday(), onDate, onDate).isEmpty();
    }

    /** 종료일이 없으면 앞으로 몇 달만 펼쳐 본다. 무기한 규칙을 끝까지 펼칠 수는 없다. */
    private static List<LocalDate> expandForCheck(ScheduleRecurrenceFreq freq,
            Set<DayOfWeek> byWeekday, LocalDate start, LocalDate endsOn) {
        return ScheduleInstanceDates.generate(freq, byWeekday, start,
                (endsOn != null) ? endsOn : start.plusMonths(RULE_CHECK_MONTHS));
    }
}
