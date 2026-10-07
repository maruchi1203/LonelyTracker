package com.lonelytracker.backend.habit.controller;

import com.lonelytracker.backend.habit.dto.HabitCategoryReorderRequest;
import com.lonelytracker.backend.habit.dto.HabitCategoryRequest;
import com.lonelytracker.backend.habit.dto.HabitCategoryResponse;
import com.lonelytracker.backend.habit.service.HabitCategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * 습관을 묶는 카테고리.
 * 습관과 길을 나눠 두어, 습관이 하나도 없는 카테고리도 화면이 받아 그릴 수 있다.
 */
@RestController
@RequestMapping("/api/habit-categories")
@RequiredArgsConstructor
public class HabitCategoryController {

    private final HabitCategoryService habitCategoryService;

    /** 카테고리 전부. 화면에 늘어놓을 차례로 온다 */
    @GetMapping
    public List<HabitCategoryResponse> findAll() {
        return habitCategoryService.findAll();
    }

    @PostMapping
    public ResponseEntity<HabitCategoryResponse> create(
            @Valid @RequestBody HabitCategoryRequest request) {
        HabitCategoryResponse created = habitCategoryService.create(request);
        return ResponseEntity.created(URI.create("/api/habit-categories/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    public HabitCategoryResponse rename(@PathVariable Long id,
            @Valid @RequestBody HabitCategoryRequest request) {
        return habitCategoryService.rename(id, request);
    }

    /**
     * 카테고리 차례를 다시 정함
     * 받은 차례대로 0부터 번호를 매김
     */
    @PatchMapping("/order")
    public ResponseEntity<Void> reorder(
            @Valid @RequestBody HabitCategoryReorderRequest request) {
        habitCategoryService.reorder(request.ids());
        return ResponseEntity.noContent().build();
    }

    /** 카테고리와 그 안의 습관을 통째로 지움. 지난 기록도 함께 사라짐 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        habitCategoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
