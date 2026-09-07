package com.lonelytracker.backend.schedule.controller;

import com.lonelytracker.backend.schedule.dto.ScheduleInstanceUpdateRequest;
import com.lonelytracker.backend.ai.ParsedSchedule;
import com.lonelytracker.backend.schedule.dto.ScheduleCompletionRequest;
import com.lonelytracker.backend.schedule.dto.ScheduleCreateRequest;
import com.lonelytracker.backend.schedule.dto.ScheduleDetailResponse;
import com.lonelytracker.backend.schedule.dto.ScheduleRecurringResponse;
import com.lonelytracker.backend.schedule.dto.ScheduleParseRequest;
import com.lonelytracker.backend.schedule.dto.ScheduleResponse;
import com.lonelytracker.backend.schedule.dto.ScheduleStatusRequest;
import com.lonelytracker.backend.schedule.dto.ScheduleUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
import java.time.LocalDateTime;
import java.util.List;
import com.lonelytracker.backend.schedule.domain.ScheduleDeleteScope;
import com.lonelytracker.backend.schedule.domain.ScheduleStatus;
import com.lonelytracker.backend.schedule.service.ScheduleInstanceService;
import com.lonelytracker.backend.schedule.service.ScheduleParseService;
import com.lonelytracker.backend.schedule.service.ScheduleService;

/**
 * 일정과 회차의 HTTP 진입점.
 * 단일과 반복이 같은 엔드포인트를 쓰고, 회차는 {@code /{id}/instances/{date}} 로 가리킨다.
 */
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final ScheduleInstanceService scheduleInstanceService;
    private final ScheduleParseService scheduleParseService;

    /**
     * 일자, 상태, 태그 기반 검색
     * 
     * @param from   시작일자
     * @param to     종료일자
     * @param status 일정 상태 {@link ScheduleStatus}
     * @param tag    태그 하나
     * @return 기간 안의 회차 목록 {@link ScheduleResponse}
     */
    @GetMapping
    public List<ScheduleResponse> search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) ScheduleStatus status,
            @RequestParam(required = false) String tag) {
        return scheduleService.search(from, to, status, tag);
    }

    /**
     * 이미 쓴 적 있는 태그 이름
     * 입력 자동완성이 쓴다
     */
    @GetMapping("/tags")
    public List<String> findTagNames() {
        return scheduleService.findTagNames();
    }

    /**
     * 반복 일정의 목록과 달성률
     */
    @GetMapping("/recurring")
    public List<ScheduleRecurringResponse> findRecurring() {
        return scheduleService.findRecurring();
    }

    /**
     * 일정 하나를 돌려줌
     * 반복 규칙이 함께 실려 수정 폼이 읽을 수 있음
     *
     * @param id 일정 ID
     * @return 일정 자체의 값 {@link ScheduleDetailResponse}
     */
    @GetMapping("/{id}")
    public ScheduleDetailResponse findById(@PathVariable Long id) {
        return scheduleService.findById(id);
    }

    /**
     * 자연어를 일정 초안으로 바꿈
     * 저장은 사용자가 확인한 뒤 POST /api/schedules로 함
     * 
     * @param request
     * @return
     */
    @PostMapping("/parse")
    public ParsedSchedule parse(@Valid @RequestBody ScheduleParseRequest request) {
        return scheduleParseService.parse(request.text());
    }

    /** recurrence 를 주면 반복, 안 주면 1회성이다. */
    @PostMapping
    public ResponseEntity<ScheduleResponse> create(@Valid @RequestBody ScheduleCreateRequest request) {
        ScheduleResponse created = scheduleService.create(request);
        return ResponseEntity
                .created(URI.create("/api/schedules/" + created.id()))
                .body(created);
    }

    /** 앞으로 전부 수정. 이미 손댄 회차는 그대로 둔다. */
    @PutMapping("/{id}")
    public ScheduleResponse update(@PathVariable Long id,
            @Valid @RequestBody ScheduleUpdateRequest request) {
        return scheduleService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            // 기본값은 그만두기다. 빠뜨렸을 때 과거 기록이 날아가면 안 된다
            @RequestParam(defaultValue = "FUTURE") ScheduleDeleteScope scope) {
        scheduleService.delete(id, scope);
        return ResponseEntity.noContent().build();
    }

    // --- 회차 -------------------------------------------------------------

    /** 완료·건너뛰기. 기록이 없으면 여기서 처음 생긴다. */
    @PatchMapping("/{id}/instances/{onDate}/status")
    public ScheduleResponse changeInstanceStatus(
            @PathVariable Long id,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate onDate,
            @Valid @RequestBody ScheduleStatusRequest request) {
        return scheduleInstanceService.changeStatus(id, onDate, request.status());
    }

    /**
     * 1회성 일정을 완료하거나 되돌린다
     * 습관은 회차마다 상태를 갖는다
     */
    @PatchMapping("/{id}/completion")
    public ScheduleResponse changeCompletion(
            @PathVariable Long id,
            @Valid @RequestBody ScheduleCompletionRequest request) {
        return scheduleService.changeCompletion(id, request.completed());
    }

    /** 이 회차만 수정. null 을 준 필드는 일정 값으로 되돌아간다. */
    @PutMapping("/{id}/instances/{onDate}")
    public ScheduleResponse updateInstance(
            @PathVariable Long id,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate onDate,
            @Valid @RequestBody ScheduleInstanceUpdateRequest request) {
        return scheduleInstanceService.updateOne(id, onDate, request);
    }
}
