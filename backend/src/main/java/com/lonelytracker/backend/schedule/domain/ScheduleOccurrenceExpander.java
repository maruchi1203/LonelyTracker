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
 * 일정을 회차로 펼치고 수행 기록을 덮어쓴다. DB 를 타지 않는 순수 계산이다.
 * <p>
 * 반복 규칙이 없으면 <b>1회짜리 일정</b>으로 취급해 회차 하나를 낸다.
 * 그래서 단일 일정과 반복 회차가 같은 모양으로 나온다.
 * <p>
 * 기록이 없는 회차는 PLANNED 로 본다. 행이 없다는 것이
 * "아직 아무것도 안 했다" 는 뜻이므로 자연스럽다.
 */
public final class ScheduleOccurrenceExpander {

    private ScheduleOccurrenceExpander() {
    }

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

        // 범위 밖 날짜에서 미뤄져 들어온 회차. 위 루프는 onDate 기준이라 이걸 못 잡는다.
        // 8/31 을 9/2 로 미루면 9월 조회에서 여기로 잡힌다.
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

    /** 규칙이 없으면 회차는 하나뿐이다 — 그 일정 자신의 날짜. */
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

    /** 일정 값에 회차 기록을 덮어쓴다. 기록이 null 이면 일정 값 그대로. */
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

    private static String pick(String override, String fallback) {
        return (override != null) ? override : fallback;
    }

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
