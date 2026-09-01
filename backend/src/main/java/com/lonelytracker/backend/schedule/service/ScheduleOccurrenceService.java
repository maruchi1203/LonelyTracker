package com.lonelytracker.backend.schedule.service;

import com.lonelytracker.backend.common.exception.NotFoundException;
import com.lonelytracker.backend.schedule.dto.OccurrenceUpdateRequest;
import com.lonelytracker.backend.schedule.dto.ScheduleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.lonelytracker.backend.schedule.domain.ScheduleOccurrenceExpander;
import com.lonelytracker.backend.schedule.domain.ScheduleStatus;
import com.lonelytracker.backend.schedule.entity.ScheduleEntity;
import com.lonelytracker.backend.schedule.entity.ScheduleProgressEntity;
import com.lonelytracker.backend.schedule.entity.ScheduleRecurEntity;
import com.lonelytracker.backend.schedule.repository.ScheduleProgressRepository;
import com.lonelytracker.backend.schedule.repository.ScheduleRecurRepository;

/**
 * 회차 단위 동작 — 완료 · 건너뛰기 · 연기 · 이 회차만 수정.
 * <p>
 * 손댄 회차만 {@link ScheduleProgressEntity} 행이 된다.
 * 회차는 {@code scheduleId + onDate} 로 식별한다. 단일 일정도 같은 방식이다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleOccurrenceService {

    private final ScheduleProgressRepository progressRepository;
    private final ScheduleRecurRepository recurRepository;
    private final ScheduleService scheduleService;

    @Transactional
    public ScheduleResponse changeStatus(Long scheduleId, LocalDate onDate, ScheduleStatus status) {
        ScheduleProgressEntity progress = getOrCreate(scheduleId, onDate);
        progress.changeStatus(status);
        return toResponse(progressRepository.saveAndFlush(progress));
    }

    @Transactional
    public ScheduleResponse postpone(Long scheduleId, LocalDate onDate, LocalDateTime to) {
        ScheduleProgressEntity progress = getOrCreate(scheduleId, onDate);
        Integer minutes = progress.getSchedule().getDurationMinutes();

        progress.postponeTo(to, (minutes == null) ? null : Duration.ofMinutes(minutes));
        return toResponse(progressRepository.saveAndFlush(progress));
    }

    /**
     * 이 회차만 수정. null 을 준 필드는 일정 값으로 되돌아간다.
     * 그래야 개별 수정을 취소할 수단이 생긴다.
     */
    @Transactional
    public ScheduleResponse updateOne(Long scheduleId, LocalDate onDate,
                                      OccurrenceUpdateRequest request) {
        if (request.startAt() != null && request.endAt() != null
                && request.endAt().isBefore(request.startAt())) {
            throw new IllegalArgumentException("endAt은 startAt보다 이를 수 없습니다");
        }

        ScheduleProgressEntity progress = getOrCreate(scheduleId, onDate);
        progress.overrideFields(
                request.title(), request.description(),
                request.startAt(), request.endAt(), request.category());
        return toResponse(progressRepository.saveAndFlush(progress));
    }

    /**
     * 기록이 없으면 만든다.
     * <p>
     * 규칙이 만들어내지 않는 날짜는 회차가 아니므로 404 다. 이 검사를 빼면
     * 아무 날짜에나 기록이 생겨서 규칙에 없는 유령 회차가 나타난다.
     */
    private ScheduleProgressEntity getOrCreate(Long scheduleId, LocalDate onDate) {
        ScheduleEntity schedule = scheduleService.getOwnedOrThrow(scheduleId);

        if (!scheduleService.occursOn(schedule, onDate)) {
            throw new NotFoundException(
                    "그 날짜에는 회차가 없습니다. scheduleId=" + scheduleId + ", date=" + onDate);
        }

        return progressRepository.findByScheduleIdAndOnDate(scheduleId, onDate)
                .orElseGet(() -> ScheduleProgressEntity.builder()
                        .schedule(schedule)
                        .onDate(onDate)
                        .status(ScheduleStatus.PLANNED)
                        .build());
    }

    /**
     * 회차 하나를 응답으로 만든다.
     * <p>
     * 전개 창을 <b>onDate 당일로 한정</b>하는 것이 중요하다. 하루라도 넘기면
     * 매일 반복에서 회차가 둘 잡히고, 미뤄서 뒤로 간 회차가 정렬에서 밀려
     * 엉뚱한 회차가 반환된다.
     */
    private ScheduleResponse toResponse(ScheduleProgressEntity progress) {
        LocalDate onDate = progress.getOnDate();
        ScheduleEntity schedule = progress.getSchedule();

        Map<Long, ScheduleRecurEntity> recurs = new HashMap<>();
        recurRepository.findById(schedule.getId())
                .ifPresent(r -> recurs.put(schedule.getId(), r));

        return ScheduleOccurrenceExpander.expand(
                        List.of(schedule), recurs, List.of(progress),
                        onDate.atStartOfDay(), onDate.atTime(LocalTime.MAX)).stream()
                .filter(r -> onDate.equals(r.occurrenceDate()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "회차를 전개하지 못했습니다. onDate=" + onDate));
    }
}
