package com.lonelytracker.backend.schedule;

import com.lonelytracker.backend.common.exception.NotFoundException;
import com.lonelytracker.backend.schedule.dto.ScheduleCreateRequest;
import com.lonelytracker.backend.schedule.dto.ScheduleResponse;
import com.lonelytracker.backend.schedule.dto.ScheduleUpdateRequest;
import com.lonelytracker.backend.schedule.spec.ScheduleSpecs;
import com.lonelytracker.backend.user.CurrentUserProvider;
import com.lonelytracker.backend.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    /**
     * 조건이 없을 때 회차를 펼칠 기본 범위.
     * 회차를 미리 만들지 않으므로 "어디까지 펼칠지" 를 반드시 정해야 한다.
     * 단일 일정은 이 범위와 무관하게 전부 나온다.
     */
    private static final int DEFAULT_PAST_MONTHS = 1;
    private static final int DEFAULT_FUTURE_MONTHS = 3;

    private final ScheduleRepository scheduleRepository;
    private final ScheduleSeriesRepository seriesRepository;
    private final ScheduleOverrideRepository overrideRepository;
    private final CurrentUserProvider currentUserProvider;

    public List<ScheduleResponse> search(LocalDateTime from, LocalDateTime to,
                                         ScheduleStatus status, String category) {
        Long userId = currentUserProvider.get().getId();

        Specification<Schedule> spec = Specification
                .allOf(ScheduleSpecs.ownedBy(userId))
                .and(ScheduleSpecs.startAtFrom(from))
                .and(ScheduleSpecs.startAtTo(to))
                .and(ScheduleSpecs.hasStatus(status))
                .and(ScheduleSpecs.hasCategory(category));

        List<ScheduleResponse> result = new ArrayList<>(
                scheduleRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "startAt")).stream()
                        .map(ScheduleResponse::from)
                        .toList());

        result.addAll(expandSeries(userId, from, to, status, category));
        result.sort(Comparator.comparing(ScheduleResponse::startAt));
        return result;
    }

    /**
     * 반복 규칙을 회차로 펼친다.
     * <p>
     * status·category 필터는 전개 후에 건다. 회차의 상태는 override 에 있고
     * 분류는 시리즈와 override 중 어느 쪽이 이길지 병합해 봐야 알기 때문이다.
     */
    private List<ScheduleResponse> expandSeries(Long userId, LocalDateTime from, LocalDateTime to,
                                                ScheduleStatus status, String category) {
        LocalDateTime windowFrom = (from != null)
                ? from : LocalDateTime.now().minusMonths(DEFAULT_PAST_MONTHS);
        LocalDateTime windowTo = (to != null)
                ? to : LocalDateTime.now().plusMonths(DEFAULT_FUTURE_MONTHS);

        List<ScheduleSeries> series = seriesRepository.findActiveIn(
                userId, windowFrom.toLocalDate(), windowTo.toLocalDate());
        if (series.isEmpty()) {
            return List.of();
        }

        List<Long> ids = series.stream().map(ScheduleSeries::getId).toList();
        List<ScheduleOverride> overrides = overrideRepository.findInRange(
                ids, windowFrom.toLocalDate(), windowTo.toLocalDate(), windowFrom, windowTo);

        return OccurrenceExpander.expand(series, overrides, windowFrom, windowTo).stream()
                .filter(r -> status == null || r.status() == status)
                .filter(r -> category == null || category.isBlank()
                        || category.strip().equals(r.category()))
                .toList();
    }

    public ScheduleResponse findById(Long id) {
        return ScheduleResponse.from(getOrThrow(id));
    }

    @Transactional
    public ScheduleResponse create(ScheduleCreateRequest request) {
        validatePeriod(request.startAt(), request.endAt());

        Schedule schedule = Schedule.builder()
                .user(currentUserProvider.get())
                .title(request.title())
                .description(request.description())
                .startAt(request.startAt())
                .endAt(request.endAt())
                .allDay(Boolean.TRUE.equals(request.allDay()))
                .category(normalizeCategory(request.category()))
                .build();

        return ScheduleResponse.from(scheduleRepository.save(schedule));
    }

    @Transactional
    public ScheduleResponse update(Long id, ScheduleUpdateRequest request) {
        validatePeriod(request.startAt(), request.endAt());

        Schedule schedule = getOrThrow(id);
        schedule.update(
                request.title(),
                request.description(),
                request.startAt(),
                request.endAt(),
                Boolean.TRUE.equals(request.allDay()),
                normalizeCategory(request.category())
        );
        // @LastModifiedDate는 flush 시점에 채워진다. 응답에 갱신된 updatedAt을 담으려면 먼저 flush.
        return ScheduleResponse.from(scheduleRepository.saveAndFlush(schedule));
    }

    @Transactional
    public ScheduleResponse changeStatus(Long id, ScheduleStatus status) {
        Schedule schedule = getOrThrow(id);
        schedule.changeStatus(status);
        return ScheduleResponse.from(scheduleRepository.saveAndFlush(schedule));
    }

    /**
     * 단일 일정을 다른 시각으로 미룬다. 회차 연기와 같은 의미다.
     * <p>
     * 반복 회차는 override 가 onDate 를 들고 있어 원래 날짜가 보존되지만,
     * 단일 일정은 행을 옮기므로 originalStartAt 이 그 역할을 한다.
     */
    @Transactional
    public ScheduleResponse postpone(Long id, LocalDateTime to) {
        Schedule schedule = getOrThrow(id);
        schedule.postponeTo(to);
        return ScheduleResponse.from(scheduleRepository.saveAndFlush(schedule));
    }

    @Transactional
    public void delete(Long id) {
        scheduleRepository.delete(getOrThrow(id));
    }

    /** 다른 사용자의 일정은 없는 것으로 취급한다. */
    private Schedule getOrThrow(Long id) {
        Long userId = currentUserProvider.get().getId();
        return scheduleRepository.findById(id)
                .filter(schedule -> ownerIdOf(schedule).equals(userId))
                .orElseThrow(() -> new NotFoundException("일정을 찾을 수 없습니다. id=" + id));
    }

    private Long ownerIdOf(Schedule schedule) {
        User owner = schedule.getUser();
        return owner.getId();
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
