package com.lonelytracker.backend.schedule;

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

/**
 * 시리즈 규칙을 회차로 펼치고 override 를 덮어쓴다. DB 를 타지 않는 순수 계산이다.
 * <p>
 * override 가 없는 회차는 PLANNED 로 본다. 행이 없다는 것이
 * "아직 아무것도 안 했다" 는 뜻이므로 자연스럽다.
 */
public final class OccurrenceExpander {

    private OccurrenceExpander() {
    }

    public static List<ScheduleResponse> expand(List<ScheduleSeries> series,
                                                List<ScheduleOverride> overrides,
                                                LocalDateTime from, LocalDateTime to) {
        Map<String, ScheduleOverride> byKey = new HashMap<>();
        for (ScheduleOverride o : overrides) {
            byKey.put(key(o.getSeries().getId(), o.getOnDate()), o);
        }

        List<ScheduleResponse> result = new ArrayList<>();
        Set<String> emitted = new HashSet<>();

        for (ScheduleSeries s : series) {
            LocalDate windowStart = maxOf(s.getStartsOn(), from.toLocalDate());
            LocalDate windowEnd = (s.getEndsOn() == null)
                    ? to.toLocalDate()
                    : minOf(s.getEndsOn(), to.toLocalDate());
            if (windowEnd.isBefore(windowStart)) {
                continue;
            }

            for (LocalDate date : OccurrenceDates.generate(
                    s.getFreq(), s.getByWeekday(), windowStart, windowEnd)) {
                String k = key(s.getId(), date);
                emitted.add(k);
                result.add(merge(s, date, byKey.get(k)));
            }
        }

        // 범위 밖 날짜에서 미뤄져 들어온 회차. 위 루프는 onDate 기준이라 이걸 못 잡는다.
        // 8/31 을 9/2 로 미루면 9월 조회에서 여기로 잡힌다.
        for (ScheduleOverride o : overrides) {
            String k = key(o.getSeries().getId(), o.getOnDate());
            if (emitted.contains(k) || o.getStartAt() == null) {
                continue;
            }
            if (!o.getStartAt().isBefore(from) && !o.getStartAt().isAfter(to)) {
                result.add(merge(o.getSeries(), o.getOnDate(), o));
            }
        }

        result.sort(Comparator.comparing(ScheduleResponse::startAt));
        return result;
    }

    /** 시리즈 템플릿에 override 를 덮어쓴다. override 가 null 이면 템플릿 그대로. */
    private static ScheduleResponse merge(ScheduleSeries s, LocalDate onDate, ScheduleOverride o) {
        LocalDateTime defaultStart = LocalDateTime.of(onDate, s.getStartTime());
        LocalDateTime startAt = (o != null && o.getStartAt() != null) ? o.getStartAt() : defaultStart;

        LocalDateTime endAt;
        if (o != null && o.getEndAt() != null) {
            endAt = o.getEndAt();
        } else if (s.getDurationMinutes() != null) {
            endAt = startAt.plus(Duration.ofMinutes(s.getDurationMinutes()));
        } else {
            endAt = null;
        }

        return new ScheduleResponse(
                null,
                s.getId(),
                onDate,
                pick(o == null ? null : o.getTitle(), s.getTitle()),
                pick(o == null ? null : o.getDescription(), s.getDescription()),
                startAt,
                endAt,
                s.isAllDay(),
                (o == null) ? ScheduleStatus.PLANNED : o.getStatus(),
                pick(o == null ? null : o.getCategory(), s.getCategory()),
                (o == null) ? 0 : o.getPostponeCount(),
                s.getCreatedAt(),
                (o == null) ? s.getUpdatedAt() : o.getUpdatedAt()
        );
    }

    private static String pick(String override, String fallback) {
        return (override != null) ? override : fallback;
    }

    private static String key(Long seriesId, LocalDate onDate) {
        return seriesId + ":" + onDate;
    }

    private static LocalDate maxOf(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private static LocalDate minOf(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }
}
