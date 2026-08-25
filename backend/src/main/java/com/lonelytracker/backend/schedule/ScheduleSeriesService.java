package com.lonelytracker.backend.schedule;

import com.lonelytracker.backend.common.exception.NotFoundException;
import com.lonelytracker.backend.schedule.dto.RecurrenceRequest;
import com.lonelytracker.backend.schedule.dto.ScheduleResponse;
import com.lonelytracker.backend.schedule.dto.ScheduleSeriesCreateRequest;
import com.lonelytracker.backend.schedule.dto.ScheduleSeriesResponse;
import com.lonelytracker.backend.schedule.dto.ScheduleSeriesUpdateRequest;
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
    private final ScheduleOverrideRepository overrideRepository;
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

    /**
     * 앞으로 전부 수정. 시리즈 1행만 바꾼다.
     * <p>
     * 회차를 미리 만드는 방식이었다면 요일이 바뀔 때 "미래 회차를 지우고 재생성" 이
     * 필요했다. 전개가 조회 시점이므로 규칙만 바꾸면 다음 조회부터 즉시 반영된다.
     */
    @Transactional
    public ScheduleSeriesResponse update(Long seriesId, ScheduleSeriesUpdateRequest request) {
        ScheduleSeries series = getOwnedOrThrow(seriesId);
        RecurrenceRequest rule = request.recurrence();
        Set<DayOfWeek> weekdays = toEnumSet(rule.byWeekday());

        // 바뀐 규칙이 회차를 하나도 못 만들면 저장하지 않는다
        List<LocalDate> dates = OccurrenceDates.generate(
                rule.freq(), weekdays, series.getStartsOn(),
                (rule.endsOn() != null)
                        ? rule.endsOn()
                        : series.getStartsOn().plusMonths(FIRST_OCCURRENCE_LOOKAHEAD_MONTHS));
        if (dates.isEmpty()) {
            throw new IllegalArgumentException("이 규칙으로는 일정이 하나도 생기지 않습니다");
        }

        series.updateRule(rule.freq(), weekdays, rule.endsOn());
        series.updateTemplate(
                request.title(),
                request.description(),
                request.startTime(),
                request.durationMinutes(),
                Boolean.TRUE.equals(request.allDay()),
                normalizeCategory(request.category()));

        seriesRepository.saveAndFlush(series);

        return new ScheduleSeriesResponse(series.getId(), firstOccurrenceOf(series, dates.get(0)));
    }

    /**
     * 그만두기 또는 전체 삭제.
     * <p>
     * 그만두기는 행을 지우지 않고 {@code endsOn} 을 오늘로 당긴다. 지난 기록을
     * 보존하면서 이후 회차만 끊는 방법이고, UPDATE 한 줄로 끝난다.
     */
    @Transactional
    public void delete(Long seriesId, SeriesDeleteScope scope) {
        ScheduleSeries series = getOwnedOrThrow(seriesId);

        if (scope == SeriesDeleteScope.ALL) {
            // DB 에 ON DELETE CASCADE 가 걸려 있지만 Hibernate 는 그것을 모른다.
            // 영속성 컨텍스트에 override 가 남은 채 시리즈를 지우면 flush 때 예외가 난다.
            overrideRepository.deleteBySeriesId(seriesId);
            seriesRepository.delete(series);
            return;
        }

        // 오늘 회차는 남긴다. 오늘 안에 아직 할 수 있기 때문이다.
        series.stopOn(LocalDate.now());
        seriesRepository.saveAndFlush(series);
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
