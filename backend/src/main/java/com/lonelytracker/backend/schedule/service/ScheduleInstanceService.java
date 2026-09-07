package com.lonelytracker.backend.schedule.service;

import com.lonelytracker.backend.common.exception.NotFoundException;
import com.lonelytracker.backend.schedule.dto.ScheduleInstanceUpdateRequest;
import com.lonelytracker.backend.schedule.dto.ScheduleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.lonelytracker.backend.schedule.domain.ScheduleInstanceExpander;
import com.lonelytracker.backend.schedule.domain.ScheduleUtil;
import com.lonelytracker.backend.schedule.domain.ScheduleStatus;
import com.lonelytracker.backend.schedule.entity.ScheduleEntity;
import com.lonelytracker.backend.schedule.entity.ScheduleProgressEntity;
import com.lonelytracker.backend.schedule.entity.ScheduleRecurEntity;
import com.lonelytracker.backend.schedule.repository.ScheduleProgressRepository;
import com.lonelytracker.backend.schedule.repository.ScheduleRecurRepository;

/**
 * 회차 하나를 다룬다 — 완료 · 건너뛰기 · 이 회차만 수정.
 * 날짜를 옮기는 것도 수정이다. 옮겨졌는지는 onDate 와 startAt 의 차이로 안다.
 * 손댄 회차만 {@link ScheduleProgressEntity} 행이 되고, 식별자는 scheduleId + onDate 다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleInstanceService {

    private final ScheduleProgressRepository progressRepository;
    private final ScheduleRecurRepository recurRepository;
    private final ScheduleService scheduleService;

    @Transactional
    public ScheduleResponse changeStatus(Long scheduleId, LocalDate onDate, ScheduleStatus status) {
        ScheduleProgressEntity progress = getOrCreate(scheduleId, onDate);
        progress.changeStatus(status);
        return toResponse(progressRepository.saveAndFlush(progress));
    }

    /**
     * 이 회차만 수정한다.
     *
     * @param request null을 준 칸은 일정 값으로 되돌아간다
     */
    @Transactional
    public ScheduleResponse updateOne(Long scheduleId, LocalDate onDate,
            ScheduleInstanceUpdateRequest request) {
        if (request.startAt() != null) {
            ScheduleUtil.validatePeriod(request.startAt(), request.endAt());
            ScheduleUtil.validateInstanceStart(onDate, request.startAt());
        }

        ScheduleProgressEntity progress = getOrCreate(scheduleId, onDate);
        progress.overrideFields(
                request.title(), request.description(),
                request.startAt(), request.endAt());
        return toResponse(progressRepository.saveAndFlush(progress));
    }

    /**
     * 회차 기록을 가져오고, 없으면 만든다.
     *
     * @throws com.lonelytracker.backend.common.exception.NotFoundException
     *                                                                      규칙이 그
     *                                                                      날짜에 회차를
     *                                                                      내지 않을 때
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
     * 전개 구간을 onDate 당일로 한정해야 매일 반복에서 회차가 둘 잡히지 않는다.
     */
    private ScheduleResponse toResponse(ScheduleProgressEntity progress) {
        LocalDate onDate = progress.getOnDate();
        ScheduleEntity schedule = progress.getSchedule();

        Map<Long, ScheduleRecurEntity> recurs = new HashMap<>();
        recurRepository.findById(schedule.getId())
                .ifPresent(r -> recurs.put(schedule.getId(), r));

        return ScheduleInstanceExpander.expand(
                List.of(schedule), recurs, List.of(progress),
                onDate.atStartOfDay(), onDate.atTime(LocalTime.MAX)).stream()
                .filter(r -> onDate.equals(r.instanceDate()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "회차를 전개하지 못했습니다. onDate=" + onDate));
    }
}
