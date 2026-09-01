package com.lonelytracker.backend.schedule.service;

import com.lonelytracker.backend.common.exception.NotFoundException;
import com.lonelytracker.backend.schedule.dto.RecurrenceRequest;
import com.lonelytracker.backend.schedule.dto.ScheduleCreateRequest;
import com.lonelytracker.backend.schedule.dto.ScheduleResponse;
import com.lonelytracker.backend.schedule.dto.ScheduleUpdateRequest;
import com.lonelytracker.backend.user.service.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.lonelytracker.backend.schedule.domain.DeleteScope;
import com.lonelytracker.backend.schedule.domain.OccurrenceDates;
import com.lonelytracker.backend.schedule.domain.OccurrenceExpander;
import com.lonelytracker.backend.schedule.domain.ScheduleStatus;
import com.lonelytracker.backend.schedule.entity.Schedule;
import com.lonelytracker.backend.schedule.entity.ScheduleProgress;
import com.lonelytracker.backend.schedule.entity.ScheduleRecur;
import com.lonelytracker.backend.schedule.repository.ScheduleProgressRepository;
import com.lonelytracker.backend.schedule.repository.ScheduleRecurRepository;
import com.lonelytracker.backend.schedule.repository.ScheduleRepository;

/**
 * 일정 자체를 다룬다 — 만들기 · 앞으로 전부 수정 · 삭제 · 조회.
 * 회차 단위 동작은 {@link ScheduleOccurrenceService} 가 맡는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    /**
     * 조건이 없을 때 회차를 펼칠 기본 범위.
     * 회차를 미리 만들지 않으므로 "어디까지 펼칠지" 를 반드시 정해야 한다.
     */
    private static final int DEFAULT_PAST_MONTHS = 1;
    private static final int DEFAULT_FUTURE_MONTHS = 3;

    private final ScheduleRepository scheduleRepository;
    private final ScheduleRecurRepository recurRepository;
    private final ScheduleProgressRepository progressRepository;
    private final CurrentUserProvider currentUserProvider;

    public List<ScheduleResponse> search(LocalDateTime from, LocalDateTime to,
                                         ScheduleStatus status, String category) {
        Long userId = currentUserProvider.get().getId();

        LocalDateTime windowFrom = (from != null)
                ? from : LocalDateTime.now().minusMonths(DEFAULT_PAST_MONTHS);
        LocalDateTime windowTo = (to != null)
                ? to : LocalDateTime.now().plusMonths(DEFAULT_FUTURE_MONTHS);

        List<Schedule> candidates = scheduleRepository.findCandidates(
                userId, windowFrom, windowTo, windowFrom.toLocalDate(), windowTo.toLocalDate());
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<Long> ids = candidates.stream().map(Schedule::getId).toList();
        Map<Long, ScheduleRecur> recurs = new HashMap<>();
        recurRepository.findByScheduleIds(ids)
                .forEach(r -> recurs.put(r.getScheduleId(), r));
        List<ScheduleProgress> progresses = progressRepository.findInRange(
                ids, windowFrom.toLocalDate(), windowTo.toLocalDate(), windowFrom, windowTo);

        // status·category 필터는 전개 후에 건다. 회차의 상태는 progress 에 있고
        // 분류는 일정과 회차 중 어느 쪽이 이길지 병합해 봐야 알기 때문이다.
        return OccurrenceExpander.expand(candidates, recurs, progresses, windowFrom, windowTo)
                .stream()
                .filter(r -> status == null || r.status() == status)
                .filter(r -> category == null || category.isBlank()
                        || category.strip().equals(r.category()))
                .toList();
    }

    /** 그 일정의 첫 회차를 돌려준다. */
    public ScheduleResponse findById(Long id) {
        Schedule schedule = getOwnedOrThrow(id);
        return firstOccurrenceOf(schedule);
    }

    @Transactional
    public ScheduleResponse create(ScheduleCreateRequest request) {
        validatePeriod(request.startAt(), request.endAt());

        Schedule schedule = scheduleRepository.save(Schedule.builder()
                .user(currentUserProvider.get())
                .title(request.title())
                .description(request.description())
                .startAt(request.startAt())
                .durationMinutes(toMinutes(request.startAt(), request.endAt()))
                .allDay(Boolean.TRUE.equals(request.allDay()))
                .category(normalizeCategory(request.category()))
                .build());

        if (request.recurrence() != null) {
            saveRecur(schedule, request.recurrence());
        }
        return firstOccurrenceOf(schedule);
    }

    /**
     * 앞으로 전부 수정. 일정 1행(과 규칙 1행)만 바뀐다.
     * <p>
     * 전개가 조회 시점이므로 요일이 바뀌어도 회차를 다시 만들 필요가 없다.
     */
    @Transactional
    public ScheduleResponse update(Long id, ScheduleUpdateRequest request) {
        validatePeriod(request.startAt(), request.endAt());

        Schedule schedule = getOwnedOrThrow(id);
        LocalDate oldDate = schedule.getStartAt().toLocalDate();

        schedule.update(
                request.title(),
                request.description(),
                request.startAt(),
                toMinutes(request.startAt(), request.endAt()),
                Boolean.TRUE.equals(request.allDay()),
                normalizeCategory(request.category()));

        applyRecurChange(schedule, request.recurrence());

        // 반복이 아닌 일정의 날짜를 옮기면 회차 기록의 onDate 도 따라가야 한다.
        // 그러지 않으면 기록이 어느 회차에도 안 붙은 유령이 된다.
        LocalDate newDate = request.startAt().toLocalDate();
        if (!recurRepository.existsById(id) && !oldDate.equals(newDate)) {
            progressRepository.findByScheduleIdAndOnDate(id, oldDate)
                    .ifPresent(p -> {
                        progressRepository.delete(p);
                        progressRepository.flush();
                        progressRepository.save(ScheduleProgress.builder()
                                .schedule(schedule)
                                .onDate(newDate)
                                .status(p.getStatus())
                                .postponeCount(p.getPostponeCount())
                                .build());
                    });
        }

        scheduleRepository.saveAndFlush(schedule);
        return firstOccurrenceOf(schedule);
    }

    /**
     * 그만두기(FUTURE) 또는 전체 삭제(ALL).
     * <p>
     * 그만두기는 행을 지우지 않고 규칙의 종료일을 오늘로 당긴다. 지난 기록을
     * 보존하면서 이후 회차만 끊는 방법이고 UPDATE 한 줄로 끝난다.
     * 반복이 아닌 일정은 끊을 미래가 없으므로 어느 쪽이든 삭제한다.
     */
    @Transactional
    public void delete(Long id, DeleteScope scope) {
        Schedule schedule = getOwnedOrThrow(id);
        ScheduleRecur recur = recurRepository.findById(id).orElse(null);

        if (scope == DeleteScope.FUTURE && recur != null) {
            recur.stopOn(LocalDate.now());
            // 그만둔 뒤로 미뤄둔 회차가 되살아나지 않게 앞으로의 기록을 지운다.
            progressRepository.deleteFutureOf(id, LocalDate.now(),
                    LocalDate.now().atTime(java.time.LocalTime.MAX));
            recurRepository.saveAndFlush(recur);
            return;
        }

        // DB 에 ON DELETE CASCADE 가 걸려 있지만 Hibernate 는 그것을 모른다.
        // 영속성 컨텍스트에 자식이 남은 채 부모를 지우면 flush 때 예외가 난다.
        progressRepository.deleteByScheduleId(id);
        if (recur != null) {
            recurRepository.delete(recur);
        }
        scheduleRepository.delete(schedule);
    }

    /** 남의 일정은 없는 것으로 취급한다. 404 로 존재 자체를 숨긴다. */
    Schedule getOwnedOrThrow(Long id) {
        Long userId = currentUserProvider.get().getId();
        return scheduleRepository.findById(id)
                .filter(s -> s.getUser().getId().equals(userId))
                .orElseThrow(() -> new NotFoundException("일정을 찾을 수 없습니다. id=" + id));
    }

    /** 그 일정이 그 날짜에 회차를 내는가. */
    boolean occursOn(Schedule schedule, LocalDate onDate) {
        if (onDate.isBefore(schedule.getStartAt().toLocalDate())) {
            return false;
        }
        ScheduleRecur recur = recurRepository.findById(schedule.getId()).orElse(null);
        if (recur == null) {
            return onDate.equals(schedule.getStartAt().toLocalDate());
        }
        if (recur.getEndsOn() != null && onDate.isAfter(recur.getEndsOn())) {
            return false;
        }
        return !OccurrenceDates.generate(
                recur.getFreq(), recur.getByWeekday(), onDate, onDate).isEmpty();
    }

    /** 규칙이 있으면 첫 회차, 없으면 그 일정 자신. */
    private ScheduleResponse firstOccurrenceOf(Schedule schedule) {
        ScheduleRecur recur = recurRepository.findById(schedule.getId()).orElse(null);
        LocalDate first = firstDateOf(schedule, recur);

        Map<Long, ScheduleRecur> recurs = new HashMap<>();
        if (recur != null) {
            recurs.put(schedule.getId(), recur);
        }
        List<ScheduleProgress> progresses = progressRepository
                .findByScheduleIdAndOnDate(schedule.getId(), first)
                .map(List::of).orElseGet(List::of);

        // 전개 창을 당일로 한정한다. 하루라도 넘기면 매일 반복에서 회차가 둘 잡히고
        // 미뤄서 뒤로 간 회차가 정렬에 밀려 엉뚱한 회차가 반환된다.
        return OccurrenceExpander.expand(
                        List.of(schedule), recurs, progresses,
                        first.atStartOfDay(), first.atTime(java.time.LocalTime.MAX))
                .stream()
                .filter(r -> first.equals(r.occurrenceDate()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "회차를 전개하지 못했습니다. scheduleId=" + schedule.getId()));
    }

    private LocalDate firstDateOf(Schedule schedule, ScheduleRecur recur) {
        LocalDate start = schedule.getStartAt().toLocalDate();
        if (recur == null) {
            return start;
        }
        List<LocalDate> dates = OccurrenceDates.generate(
                recur.getFreq(), recur.getByWeekday(), start,
                (recur.getEndsOn() != null) ? recur.getEndsOn() : start.plusMonths(2));
        if (dates.isEmpty()) {
            throw new IllegalArgumentException("이 규칙으로는 일정이 하나도 생기지 않습니다");
        }
        return dates.get(0);
    }

    private void applyRecurChange(Schedule schedule, RecurrenceRequest rule) {
        ScheduleRecur existing = recurRepository.findById(schedule.getId()).orElse(null);

        if (rule == null) {
            if (existing != null) {
                recurRepository.delete(existing);
            }
            return;
        }
        if (existing == null) {
            saveRecur(schedule, rule);
            return;
        }
        validateRule(schedule, rule);
        existing.updateRule(rule.freq(), toEnumSet(rule.byWeekday()), rule.endsOn());
        recurRepository.saveAndFlush(existing);
    }

    private void saveRecur(Schedule schedule, RecurrenceRequest rule) {
        validateRule(schedule, rule);
        recurRepository.save(ScheduleRecur.builder()
                .schedule(schedule)
                .freq(rule.freq())
                .byWeekday(toEnumSet(rule.byWeekday()))
                .endsOn(rule.endsOn())
                .build());
    }

    /** 규칙이 회차를 하나도 못 내면 저장하지 않는다. */
    private void validateRule(Schedule schedule, RecurrenceRequest rule) {
        LocalDate start = schedule.getStartAt().toLocalDate();
        List<LocalDate> dates = OccurrenceDates.generate(
                rule.freq(), toEnumSet(rule.byWeekday()), start,
                (rule.endsOn() != null) ? rule.endsOn() : start.plusMonths(2));
        if (dates.isEmpty()) {
            throw new IllegalArgumentException("이 규칙으로는 일정이 하나도 생기지 않습니다");
        }
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

    /** 빈 문자열은 미분류(null)로 통일한다. */
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
