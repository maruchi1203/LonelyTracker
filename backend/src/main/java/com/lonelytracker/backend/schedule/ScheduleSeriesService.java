package com.lonelytracker.backend.schedule;

import com.lonelytracker.backend.common.exception.NotFoundException;
import com.lonelytracker.backend.schedule.dto.RecurrenceRequest;
import com.lonelytracker.backend.schedule.dto.ScheduleResponse;
import com.lonelytracker.backend.schedule.dto.ScheduleSeriesCreateRequest;
import com.lonelytracker.backend.schedule.dto.ScheduleSeriesResponse;
import com.lonelytracker.backend.user.CurrentUserProvider;
import com.lonelytracker.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** 반복 규칙 생성. 회차는 만들지 않는다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleSeriesService {

    /** 첫 회차를 찾을 때 얼마나 앞을 볼지. 매주 반복이면 최대 7일이면 충분하다. */
    private static final int FIRST_OCCURRENCE_LOOKAHEAD_MONTHS = 2;

    private final ScheduleSeriesRepository seriesRepository;
    private final CurrentUserProvider currentUserProvider;

    @Transactional
    public ScheduleSeriesResponse create(ScheduleSeriesCreateRequest request) {
        validatePeriod(request.startAt(), request.endAt());

        RecurrenceRequest rule = request.recurrence();
        Set<DayOfWeek> weekdays = toEnumSet(rule.byWeekday());
        LocalDate startsOn = request.startAt().toLocalDate();

        // 규칙이 유효한지 여기서 확인한다. 요일 누락 등은 예외가 나고 아무것도 저장되지 않는다.
        List<LocalDate> firstDates = OccurrenceDates.generate(
                rule.freq(), weekdays, startsOn,
                (rule.endsOn() != null)
                        ? rule.endsOn()
                        : startsOn.plusMonths(FIRST_OCCURRENCE_LOOKAHEAD_MONTHS));
        if (firstDates.isEmpty()) {
            throw new IllegalArgumentException("이 규칙으로는 일정이 하나도 생기지 않습니다");
        }

        User owner = currentUserProvider.get();
        ScheduleSeries series = seriesRepository.save(ScheduleSeries.builder()
                .user(owner)
                .freq(rule.freq())
                .byWeekday(weekdays)
                .startsOn(startsOn)
                .endsOn(rule.endsOn())
                .title(request.title())
                .description(request.description())
                .startTime(request.startAt().toLocalTime())
                .durationMinutes(toMinutes(request.startAt(), request.endAt()))
                .allDay(Boolean.TRUE.equals(request.allDay()))
                .category(normalizeCategory(request.category()))
                .build());

        return new ScheduleSeriesResponse(series.getId(), firstOccurrenceOf(series, firstDates.get(0)));
    }

    /** 남의 시리즈는 없는 것으로 취급한다. 404 로 존재 자체를 숨긴다. */
    ScheduleSeries getOwnedOrThrow(Long seriesId) {
        Long userId = currentUserProvider.get().getId();
        return seriesRepository.findById(seriesId)
                .filter(s -> s.getUser().getId().equals(userId))
                .orElseThrow(() -> new NotFoundException("반복 일정을 찾을 수 없습니다. id=" + seriesId));
    }

    /**
     * 전개 창을 당일로 한정한다. 하루를 넘기면 매일 반복에서 회차가 둘 잡혀
     * 어느 것이 "첫 회차" 인지 정렬에 기대게 된다.
     */
    private ScheduleResponse firstOccurrenceOf(ScheduleSeries series, LocalDate first) {
        return OccurrenceExpander.expand(
                List.of(series), List.of(),
                first.atStartOfDay(), first.atTime(LocalTime.MAX)).get(0);
    }

    private Integer toMinutes(LocalDateTime startAt, LocalDateTime endAt) {
        return (endAt == null) ? null : (int) Duration.between(startAt, endAt).toMinutes();
    }

    /** EnumSet.copyOf 는 빈 컬렉션에 예외를 던진다. DAILY 는 요일이 비는 게 정상이다. */
    private Set<DayOfWeek> toEnumSet(Set<DayOfWeek> source) {
        Set<DayOfWeek> weekdays = EnumSet.noneOf(DayOfWeek.class);
        if (source != null) {
            weekdays.addAll(source);
        }
        return weekdays;
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        return category.strip();
    }

    private void validatePeriod(LocalDateTime startAt, LocalDateTime endAt) {
        if (endAt != null && endAt.isBefore(startAt)) {
            throw new IllegalArgumentException("endAt은 startAt보다 이를 수 없습니다");
        }
    }
}
