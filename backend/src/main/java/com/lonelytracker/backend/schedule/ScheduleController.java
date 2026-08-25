package com.lonelytracker.backend.schedule;

import com.lonelytracker.backend.schedule.dto.*;
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
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @GetMapping
    public List<ScheduleResponse> search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @RequestParam(required = false) ScheduleStatus status,
            @RequestParam(required = false) String category) {
        return scheduleService.search(from, to, status, category);
    }

    // ID 기준으로 
    @GetMapping("/{id}")
    public ScheduleResponse findById(@PathVariable Long id) {
        return scheduleService.findById(id);
    }

    @PostMapping
    public ResponseEntity<ScheduleResponse> create(@Valid @RequestBody ScheduleCreateRequest request) {
        ScheduleResponse created = scheduleService.create(request);
        return ResponseEntity
                .created(URI.create("/api/schedules/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    public ScheduleResponse update(@PathVariable Long id, @Valid @RequestBody ScheduleUpdateRequest request) {
        return scheduleService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    public ScheduleResponse changeStatus(@PathVariable Long id, @Valid @RequestBody ScheduleStatusRequest request) {
        return scheduleService.changeStatus(id, request.status());
    }

    @PatchMapping("/{id}/postpone")
    public ScheduleResponse postpone(@PathVariable Long id,
                                     @Valid @RequestBody SchedulePostponeRequest request) {
        return scheduleService.postpone(id, request.to());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        scheduleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
