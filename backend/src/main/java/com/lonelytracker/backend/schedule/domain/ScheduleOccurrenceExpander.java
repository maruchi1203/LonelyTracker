package com.lonelytracker.backend.schedule.domain;

import com.lonelytracker.backend.schedule.dto.ScheduleResponse;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.lonelytracker.backend.schedule.entity.ScheduleEntity;
import com.lonelytracker.backend.schedule.entity.ScheduleProgressEntity;
import com.lonelytracker.backend.schedule.entity.ScheduleRecurEntity;

/**
 * 일정을 회차로 펼치고 수행 기록을 덮어쓴다. DB와 스프링을 모른다.
 * 회차는 저장하지 않고 조회할 때마다 여기서 만든다.
 */
public final class ScheduleOccurrenceExpander {

    private ScheduleOccurrenceExpander() {
    }

    /**
     * 조회 구간에 들어가는 회차를 만든다.
     *
     * @param schedules         펼칠 일정들
     * @param recurByScheduleId 일정 ID로 찾는 반복 규칙. 없으면 1회성이다
     * @param progresses        수행 기록. 손댄 회차에만 있다
     * @param from              조회 시작 시각
     * @param to                조회 끝 시각
     * @return 시작 시각 순으로 정렬된 회차
     */
    public static List<ScheduleResponse> expand(List<ScheduleEntity> schedules,
            Map<Long, ScheduleRecurEntity> recurByScheduleId,
            List<ScheduleProgressEntity> progresses,
            LocalDateTime from, LocalDateTime to) {

        Map<String, ScheduleProgressEntity> byKey = new HashMap<>();
        for (ScheduleProgressEntity p : progresses) {
            byKey.put(key(p.getSchedule().getId(), p.getOnDate()), p);
        }

        List<ScheduleResponse> result = new ArrayList<>();
        Set<String> emitted = new HashSet<>();

        for (ScheduleEntity s : schedules) {
            ScheduleRecurEntity recur = recurByScheduleId.get(s.getId());
            for (LocalDate date : occurrenceDatesOf(s, recur, from, to)) {
                String k = key(s.getId(), date);
                emitted.add(k);
                result.add(merge(s, date, byKey.get(k), recur != null));
            }
        }

        // 범위 밖에서 미뤄져 들어온 회차. 위 루프는 onDate 기준이라 못 잡는다.
        // 8/31을 9/2로 미루면 9월 조회에서 여기로 잡힌다.
        for (ScheduleProgressEntity p : progresses) {
            String k = key(p.getSchedule().getId(), p.getOnDate());
            if (emitted.contains(k) || p.getStartAt() == null) {
                continue;
            }
            if (!p.getStartAt().isBefore(from) && !p.getStartAt().isAfter(to)) {
                result.add(merge(p.getSchedule(), p.getOnDate(), p,
                        recurByScheduleId.containsKey(p.getSchedule().getId())));
            }
        }

        result.sort(Comparator.comparing(ScheduleResponse::startAt));
        return result;
    }

    /**
     * 구간 안에서 이 일정이 회차를 내는 날짜들.
     *
     * @param recur null이면 회차는 일정 자신의 날짜 하나뿐이다
     */
    private static List<LocalDate> occurrenceDatesOf(ScheduleEntity s, ScheduleRecurEntity recur,
            LocalDateTime from, LocalDateTime to) {
        LocalDate firstDate = s.getStartAt().toLocalDate();

        if (recur == null) {
            boolean inWindow = !firstDate.isBefore(from.toLocalDate())
                    && !firstDate.isAfter(to.toLocalDate());
            return inWindow ? List.of(firstDate) : List.of();
        }

        LocalDate windowStart = maxOf(firstDate, from.toLocalDate());
        LocalDate windowEnd = (recur.getEndsOn() == null)
                ? to.toLocalDate()
                : minOf(recur.getEndsOn(), to.toLocalDate());
        if (windowEnd.isBefore(windowStart)) {
            return List.of();
        }
        return ScheduleOccurrenceDates.generate(recur.getFreq(), recur.getByWeekday(), windowStart, windowEnd);
    }

    /**
     * 일정 값에 회차 기록을 덮어쓴다.
     *
     * @param p         수행 기록. null이면 일정 값 그대로에 상태는 PLANNED
     * @param recurring 반복 일정의 회차인지
     */
    private static ScheduleResponse merge(ScheduleEntity s, LocalDate onDate, ScheduleProgressEntity p,
            boolean recurring) {
        LocalDateTime defaultStart = LocalDateTime.of(onDate, s.getStartAt().toLocalTime());
        LocalDateTime startAt = (p != null && p.getStartAt() != null) ? p.getStartAt() : defaultStart;

        LocalDateTime endAt;
        if (p != null && p.getEndAt() != null) {
            endAt = p.getEndAt();
        } else if (s.getDurationMinutes() != null) {
            endAt = startAt.plus(Duration.ofMinutes(s.getDurationMinutes()));
        } else {
            endAt = null;
        }

        return new ScheduleResponse(
                s.getId(),
                onDate,
                pick(p == null ? null : p.getTitle(), s.getTitle()),
                pick(p == null ? null : p.getDescription(), s.getDescription()),
                startAt,
                endAt,
                s.isAllDay(),
                recurring,
                (p == null) ? ScheduleStatus.PLANNED : p.getStatus(),
                pick(p == null ? null : p.getCategory(), s.getCategory()),
                (p == null) ? 0 : p.getPostponeCount(),
                s.getCreatedAt(),
                (p == null) ? s.getUpdatedAt() : p.getUpdatedAt());
    }

    /** 회차 값이 있으면 그것을, 없으면 일정 값을 쓴다 */
    private static String pick(String override, String fallback) {
        return (override != null) ? override : fallback;
    }

    /** 회차 식별자. 일정 ID 하나로는 반복 회차를 구분할 수 없다 */
    private static String key(Long scheduleId, LocalDate onDate) {
        return scheduleId + ":" + onDate;
    }

    private static LocalDate maxOf(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate minOf(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }
}
