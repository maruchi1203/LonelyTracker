package com.lonelytracker.backend.schedule.service;

import com.lonelytracker.backend.common.exception.NotFoundException;
import com.lonelytracker.backend.schedule.dto.RecurrenceRequest;
import com.lonelytracker.backend.schedule.dto.ScheduleCreateRequest;
import com.lonelytracker.backend.schedule.dto.ScheduleDetailResponse;
import com.lonelytracker.backend.schedule.dto.ScheduleResponse;
import com.lonelytracker.backend.schedule.dto.ScheduleUpdateRequest;
import com.lonelytracker.backend.user.service.UserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.lonelytracker.backend.schedule.domain.ScheduleDeleteScope;
import com.lonelytracker.backend.schedule.domain.ScheduleInstanceExpander;
import com.lonelytracker.backend.schedule.domain.ScheduleUtil;
import com.lonelytracker.backend.schedule.domain.ScheduleStatus;
import com.lonelytracker.backend.schedule.entity.ScheduleEntity;
import com.lonelytracker.backend.schedule.entity.ScheduleProgressEntity;
import com.lonelytracker.backend.schedule.entity.ScheduleRecurEntity;
import com.lonelytracker.backend.schedule.repository.ScheduleProgressRepository;
import com.lonelytracker.backend.schedule.repository.ScheduleRecurRepository;
import com.lonelytracker.backend.schedule.repository.ScheduleRepository;

/**
 * 일정 자체를 다룬다 — 만들기 · 앞으로 전부 수정 · 삭제 · 조회.
 * 회차 단위 동작은 {@link ScheduleInstanceService} 가 맡는다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    /** 조회 조건이 없을 때 이번 주 월요일부터 볼 주 수 */
    private static final int DEFAULT_WEEKS = 4;

    private final ScheduleRepository scheduleRepository;
    private final ScheduleRecurRepository recurRepository;
    private final ScheduleProgressRepository progressRepository;
    private final UserProvider currentUserProvider;

    /**
     * 일자, 상태, 카테고리 기반 일정 검색
     * 
     * @param from     시작일시. null이면 이번 주 월요일 0시
     * @param to       종료일시. null이면 그로부터 4주째 일요일 끝
     * @param status   일정 상태
     * @param category 일정 카테고리
     * @return 일정목록 반환 List<ScheduleResponse>
     */
    public List<ScheduleResponse> search(LocalDateTime from, LocalDateTime to,
            ScheduleStatus status, String category) {
        Long userId = currentUserProvider.get().getId();

        LocalDate monday = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDateTime windowFrom = (from != null)
                ? from
                : monday.atStartOfDay();
        LocalDateTime windowTo = (to != null)
                ? to
                : monday.plusWeeks(DEFAULT_WEEKS).minusDays(1).atTime(LocalTime.MAX);

        ScheduleUtil.validateWindow(windowFrom, windowTo);

        List<ScheduleEntity> candidates = scheduleRepository.findCandidates(
                userId, windowFrom, windowTo, windowFrom.toLocalDate(), windowTo.toLocalDate());
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<Long> ids = candidates.stream().map(ScheduleEntity::getId).toList();
        Map<Long, ScheduleRecurEntity> recurs = new HashMap<>();
        recurRepository.findByScheduleIds(ids)
                .forEach(r -> recurs.put(r.getScheduleId(), r));
        List<ScheduleProgressEntity> progresses = progressRepository.findInRange(
                ids, windowFrom.toLocalDate(), windowTo.toLocalDate(), windowFrom, windowTo);

        // 상태와 분류는 전개 후에 거른다. 회차 값이 일정 값을 덮을 수 있어 병합 뒤에야 정해진다
        return ScheduleInstanceExpander.expand(candidates, recurs, progresses, windowFrom, windowTo)
                .stream()
                .filter(r -> status == null || r.status() == status)
                .filter(r -> category == null || category.isBlank()
                        || category.strip().equals(r.category()))
                .toList();
    }

    /** 일정 자체를 돌려준다. 반복이면 규칙도 함께 실어 수정 폼이 읽을 수 있게 한다. */
    public ScheduleDetailResponse findById(Long id) {
        ScheduleEntity schedule = getOwnedOrThrow(id);
        return ScheduleDetailResponse.of(schedule, recurRepository.findById(id).orElse(null));
    }

    /**
     * 일정 등록
     * 
     * @param request 일정 등록 요청 포맷
     * @return 등록된 일정 정보
     */
    @Transactional
    public ScheduleResponse create(ScheduleCreateRequest request) {
        ScheduleUtil.validatePeriod(request.startAt(), request.endAt());

        ScheduleEntity schedule = scheduleRepository.save(ScheduleEntity.builder()
                .user(currentUserProvider.get())
                .title(request.title())
                .description(request.description())
                .startAt(request.startAt())
                .durationMinutes(ScheduleUtil.toMinutes(request.startAt(), request.endAt()))
                .allDay(Boolean.TRUE.equals(request.allDay()))
                .category(ScheduleUtil.normalizeCategory(request.category()))
                .build());

        if (request.recurrence() != null) {
            saveRecur(schedule, request.recurrence());
        }

        return firstInstanceOf(schedule);
    }

    /**
     * 앞으로의 회차를 전부 수정한다. 일정 1행과 규칙 1행만 바뀐다.
     *
     * @param request recurrence를 빼면 반복이 해제된다
     */
    @Transactional
    public ScheduleResponse update(Long id, ScheduleUpdateRequest request) {
        ScheduleUtil.validatePeriod(request.startAt(), request.endAt());

        ScheduleEntity schedule = getOwnedOrThrow(id);
        LocalDate oldDate = schedule.getStartAt().toLocalDate();

        schedule.update(
                request.title(),
                request.description(),
                request.startAt(),
                ScheduleUtil.toMinutes(request.startAt(), request.endAt()),
                Boolean.TRUE.equals(request.allDay()),
                ScheduleUtil.normalizeCategory(request.category()));

        applyRecurChange(schedule, request.recurrence());

        // 1회성 일정의 날짜를 옮기면 회차 기록의 onDate도 따라가야 한다
        LocalDate newDate = request.startAt().toLocalDate();
        if (!recurRepository.existsById(id) && !oldDate.equals(newDate)) {
            progressRepository.findByScheduleIdAndOnDate(id, oldDate)
                    .ifPresent(p -> {
                        progressRepository.delete(p);
                        progressRepository.flush();
                        progressRepository.save(ScheduleProgressEntity.builder()
                                .schedule(schedule)
                                .onDate(newDate)
                                .status(p.getStatus())
                                .postponeCount(p.getPostponeCount())
                                .build());
                    });
        }

        scheduleRepository.saveAndFlush(schedule);
        return firstInstanceOf(schedule);
    }

    /**
     * 일정을 지운다.
     *
     * @param scope FUTURE는 규칙의 종료일을 오늘로 당겨 지난 기록을 남기고,
     *              ALL은 전부 지운다. 1회성 일정은 어느 쪽이든 삭제된다
     */
    @Transactional
    public void delete(Long id, ScheduleDeleteScope scope) {
        ScheduleEntity schedule = getOwnedOrThrow(id);
        ScheduleRecurEntity recur = recurRepository.findById(id).orElse(null);

        if (scope == ScheduleDeleteScope.FUTURE && recur != null) {
            recur.stopOn(LocalDate.now());
            // 앞으로 미뤄둔 회차가 되살아나지 않게 지운다
            progressRepository.deleteFutureOf(id, LocalDate.now(),
                    LocalDate.now().atTime(java.time.LocalTime.MAX));
            recurRepository.saveAndFlush(recur);
            return;
        }

        // DB의 ON DELETE CASCADE를 Hibernate는 모른다. 자식을 먼저 지워야 flush에서 안 터진다
        progressRepository.deleteByScheduleId(id);
        if (recur != null) {
            recurRepository.delete(recur);
        }
        scheduleRepository.delete(schedule);
    }

    // -------------------- Helper --------------------

    /** 남의 일정은 없는 것으로 취급한다 */
    ScheduleEntity getOwnedOrThrow(Long id) {
        Long userId = currentUserProvider.get().getId();
        return scheduleRepository.findById(id)
                .filter(s -> s.getUser().getId().equals(userId))
                .orElseThrow(() -> new NotFoundException("일정을 찾을 수 없습니다. id=" + id));
    }

    /** 그 일정이 그 날짜에 회차를 내는가. */
    boolean occursOn(ScheduleEntity schedule, LocalDate onDate) {
        return ScheduleUtil.occursOn(schedule,
                recurRepository.findById(schedule.getId()).orElse(null), onDate);
    }

    /** 규칙이 있으면 첫 회차, 없으면 그 일정 자신. */
    private ScheduleResponse firstInstanceOf(ScheduleEntity schedule) {
        ScheduleRecurEntity recur = recurRepository.findById(schedule.getId()).orElse(null);
        LocalDate first = ScheduleUtil.firstDateOf(schedule, recur);

        Map<Long, ScheduleRecurEntity> recurs = new HashMap<>();
        if (recur != null) {
            recurs.put(schedule.getId(), recur);
        }
        List<ScheduleProgressEntity> progresses = progressRepository
                .findByScheduleIdAndOnDate(schedule.getId(), first)
                .map(List::of).orElseGet(List::of);

        // 전개 구간을 당일로 한정한다. 넓히면 매일 반복에서 회차가 둘 이상 잡힌다
        return ScheduleInstanceExpander.expand(
                List.of(schedule), recurs, progresses,
                first.atStartOfDay(), first.atTime(java.time.LocalTime.MAX))
                .stream()
                .filter(r -> first.equals(r.instanceDate()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "회차를 전개하지 못했습니다. scheduleId=" + schedule.getId()));
    }

    private void applyRecurChange(ScheduleEntity schedule, RecurrenceRequest rule) {
        ScheduleRecurEntity existing = recurRepository.findById(schedule.getId()).orElse(null);

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
        ScheduleUtil.validateRule(schedule, rule);
        existing.updateRule(rule.freq(), ScheduleUtil.toWeekdaySet(rule.byWeekday()), rule.endsOn());
        recurRepository.saveAndFlush(existing);
    }

    private void saveRecur(ScheduleEntity schedule, RecurrenceRequest rule) {
        ScheduleUtil.validateRule(schedule, rule);
        recurRepository.save(ScheduleRecurEntity.builder()
                .schedule(schedule)
                .freq(rule.freq())
                .byWeekday(ScheduleUtil.toWeekdaySet(rule.byWeekday()))
                .endsOn(rule.endsOn())
                .build());
    }
}
