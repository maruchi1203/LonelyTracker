package com.lonelytracker.backend.schedule;

import com.lonelytracker.backend.common.exception.NotFoundException;
import com.lonelytracker.backend.schedule.dto.ScheduleResponse;
import com.lonelytracker.backend.schedule.dto.ScheduleUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 회차 단위 동작. 손댄 회차만 override 행이 된다.
 * <p>
 * 회차는 {@code seriesId + onDate} 로 식별한다. 행이 없으므로 id 가 없다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleOccurrenceService {

    private final ScheduleOverrideRepository overrideRepository;
    private final ScheduleSeriesService scheduleSeriesService;

    @Transactional
    public ScheduleResponse changeStatus(Long seriesId, LocalDate onDate, ScheduleStatus status) {
        ScheduleOverride override = getOrCreate(seriesId, onDate);
        override.changeStatus(status);
        return toResponse(overrideRepository.saveAndFlush(override));
    }

    @Transactional
    public ScheduleResponse postpone(Long seriesId, LocalDate onDate, LocalDateTime to) {
        ScheduleOverride override = getOrCreate(seriesId, onDate);
        ScheduleSeries series = override.getSeries();

        Duration length = (series.getDurationMinutes() == null)
                ? null : Duration.ofMinutes(series.getDurationMinutes());

        override.postponeTo(to, length);
        return toResponse(overrideRepository.saveAndFlush(override));
    }

    /**
     * 이 회차만 수정.
     * <p>
     * null 을 준 필드는 시리즈 값으로 되돌아간다. 덮어쓰기 테이블이므로
     * "값이 없다" 가 곧 "템플릿을 따른다" 는 뜻이다.
     */
    @Transactional
    public ScheduleResponse updateOne(Long seriesId, LocalDate onDate,
                                      ScheduleUpdateRequest request) {
        if (request.startAt() != null && request.endAt() != null
                && request.endAt().isBefore(request.startAt())) {
            throw new IllegalArgumentException("endAt은 startAt보다 이를 수 없습니다");
        }

        ScheduleOverride override = getOrCreate(seriesId, onDate);
        override.overrideFields(
                request.title(), request.description(),
                request.startAt(), request.endAt(), request.category());
        return toResponse(overrideRepository.saveAndFlush(override));
    }

    /**
     * override 가 없으면 만든다.
     * <p>
     * 규칙이 만들어내지 않는 날짜는 회차가 아니므로 404 다. 이 검사를 빼면
     * 아무 날짜에나 override 가 생겨서 규칙에 없는 유령 회차가 나타난다.
     */
    private ScheduleOverride getOrCreate(Long seriesId, LocalDate onDate) {
        ScheduleSeries series = scheduleSeriesService.getOwnedOrThrow(seriesId);

        if (!occursOn(series, onDate)) {
            throw new NotFoundException(
                    "그 날짜에는 회차가 없습니다. seriesId=" + seriesId + ", date=" + onDate);
        }

        return overrideRepository.findBySeriesIdAndOnDate(seriesId, onDate)
                .orElseGet(() -> ScheduleOverride.builder()
                        .series(series)
                        .onDate(onDate)
                        .status(ScheduleStatus.PLANNED)
                        .build());
    }

    /** 그 날짜가 이 규칙이 만들어내는 회차인가. */
    private boolean occursOn(ScheduleSeries series, LocalDate onDate) {
        if (onDate.isBefore(series.getStartsOn())) {
            return false;
        }
        if (series.getEndsOn() != null && onDate.isAfter(series.getEndsOn())) {
            return false;
        }
        return !OccurrenceDates.generate(
                series.getFreq(), series.getByWeekday(), onDate, onDate).isEmpty();
    }

    /**
     * 회차 하나를 응답으로 만든다.
     * <p>
     * 전개 창을 <b>onDate 당일로 한정</b>하는 것이 중요하다. 하루라도 넘기면
     * 매일 반복에서 회차가 둘 잡히고, 미뤄서 뒤로 간 회차가 정렬에서 밀려
     * 엉뚱한 회차가 반환된다. 창이 당일이면 결과가 정확히 하나다.
     * <p>
     * 미뤄서 startAt 이 창 밖으로 나가도 문제없다. 전개는 onDate 기준으로 돌기 때문이다.
     */
    private ScheduleResponse toResponse(ScheduleOverride override) {
        LocalDate onDate = override.getOnDate();
        return OccurrenceExpander.expand(
                        List.of(override.getSeries()), List.of(override),
                        onDate.atStartOfDay(), onDate.atTime(LocalTime.MAX)).stream()
                .filter(r -> onDate.equals(r.occurrenceDate()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "회차를 전개하지 못했습니다. onDate=" + onDate));
    }
}
