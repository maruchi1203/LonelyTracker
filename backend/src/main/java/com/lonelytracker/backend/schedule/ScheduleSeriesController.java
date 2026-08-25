package com.lonelytracker.backend.schedule;

import com.lonelytracker.backend.schedule.dto.SchedulePostponeRequest;
import com.lonelytracker.backend.schedule.dto.ScheduleResponse;
import com.lonelytracker.backend.schedule.dto.ScheduleSeriesCreateRequest;
import com.lonelytracker.backend.schedule.dto.ScheduleSeriesResponse;
import com.lonelytracker.backend.schedule.dto.ScheduleSeriesUpdateRequest;
import com.lonelytracker.backend.schedule.dto.ScheduleStatusRequest;
import com.lonelytracker.backend.schedule.dto.ScheduleUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;

/**
 * 반복 규칙과 그 회차.
 * <p>
 * 회차는 행이 아니므로 id 가 없다. {@code seriesId + onDate} 로 식별하고,
 * 그래서 경로가 {@code /api/series/{id}/occurrences/{date}} 모양이 된다.
 */
@RestController
@RequestMapping("/api/series")
@RequiredArgsConstructor
public class ScheduleSeriesController {

    private final ScheduleSeriesService scheduleSeriesService;
    private final ScheduleOccurrenceService scheduleOccurrenceService;

    @PostMapping
    public ResponseEntity<ScheduleSeriesResponse> create(
            @Valid @RequestBody ScheduleSeriesCreateRequest request) {
        ScheduleSeriesResponse created = scheduleSeriesService.create(request);
        return ResponseEntity
                .created(URI.create("/api/series/" + created.seriesId()))
                .body(created);
    }

    /** 앞으로 전부 수정. 시리즈 1행만 바뀐다. */
    @PutMapping("/{seriesId}")
    public ScheduleSeriesResponse update(@PathVariable Long seriesId,
                                         @Valid @RequestBody ScheduleSeriesUpdateRequest request) {
        return scheduleSeriesService.update(seriesId, request);
    }

    /** 그만두기(FUTURE) 또는 전체 삭제(ALL). */
    @DeleteMapping("/{seriesId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long seriesId,
            // 기본값은 그만두기다. 빠뜨렸을 때 과거 기록이 날아가면 안 된다.
            @RequestParam(defaultValue = "FUTURE") SeriesDeleteScope scope) {
        scheduleSeriesService.delete(seriesId, scope);
        return ResponseEntity.noContent().build();
    }

    /** 이 회차만 수정. 다른 회차와 시리즈 템플릿은 그대로다. */
    @PutMapping("/{seriesId}/occurrences/{onDate}")
    public ScheduleResponse updateOccurrence(
            @PathVariable Long seriesId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate onDate,
            @Valid @RequestBody ScheduleUpdateRequest request) {
        return scheduleOccurrenceService.updateOne(seriesId, onDate, request);
    }

    /** 완료·건너뛰기. 회차에 override 가 없으면 여기서 처음 생긴다. */
    @PatchMapping("/{seriesId}/occurrences/{onDate}/status")
    public ScheduleResponse changeOccurrenceStatus(
            @PathVariable Long seriesId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate onDate,
            @Valid @RequestBody ScheduleStatusRequest request) {
        return scheduleOccurrenceService.changeStatus(seriesId, onDate, request.status());
    }

    /** 연기. onDate 는 그대로 두고 startAt 만 옮긴다. */
    @PatchMapping("/{seriesId}/occurrences/{onDate}/postpone")
    public ScheduleResponse postponeOccurrence(
            @PathVariable Long seriesId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate onDate,
            @Valid @RequestBody SchedulePostponeRequest request) {
        return scheduleOccurrenceService.postpone(seriesId, onDate, request.to());
    }
}
