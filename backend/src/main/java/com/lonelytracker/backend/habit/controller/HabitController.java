package com.lonelytracker.backend.habit.controller;

import com.lonelytracker.backend.habit.dto.HabitCreateRequest;
import com.lonelytracker.backend.habit.dto.HabitLogRequest;
import com.lonelytracker.backend.habit.dto.HabitResponse;
import com.lonelytracker.backend.habit.dto.HabitUpdateRequest;
import com.lonelytracker.backend.habit.service.HabitService;
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
import java.util.List;

/**
 * 습관일지.
 * 일정과 다른 길을 쓴다. 탭을 떼어내도 일정 쪽이 흔들리지 않게 한다.
 */
@RestController
@RequestMapping("/api/habits")
@RequiredArgsConstructor
public class HabitController {

    private final HabitService habitService;

    /**
     * 습관 목록과 그 구간의 기록
     *
     * @param from null이면 오늘로부터 2주 전
     * @param to   null이면 오늘
     */
    @GetMapping
    public List<HabitResponse> findAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return habitService.findAll(from, to);
    }

    @PostMapping
    public ResponseEntity<HabitResponse> create(@Valid @RequestBody HabitCreateRequest request) {
        HabitResponse created = habitService.create(request);
        return ResponseEntity.created(URI.create("/api/habits/" + created.id())).body(created);
    }

    @PutMapping("/{id}")
    public HabitResponse update(@PathVariable Long id,
            @Valid @RequestBody HabitUpdateRequest request) {
        return habitService.update(id, request);
    }

    /**
     * 그날 해냈는지 표시함
     * done 이 false 면 그날 기록을 지움
     */
    @PutMapping("/{id}/logs/{onDate}")
    public ResponseEntity<Void> log(@PathVariable Long id,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate onDate,
            @Valid @RequestBody HabitLogRequest request) {
        habitService.log(id, onDate, request);
        return ResponseEntity.noContent().build();
    }

    /** 그만두거나 다시 시작함. 지난 기록은 남음 */
    @PatchMapping("/{id}/archived")
    public HabitResponse changeArchived(@PathVariable Long id,
            @RequestParam boolean archived) {
        return habitService.changeArchived(id, archived);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        habitService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
