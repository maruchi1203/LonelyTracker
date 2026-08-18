package com.lonelytracker.backend.category;

import com.lonelytracker.backend.category.dto.CategoryAppearanceRequest;
import com.lonelytracker.backend.category.dto.CategoryCreateRequest;
import com.lonelytracker.backend.category.dto.CategoryRenameRequest;
import com.lonelytracker.backend.category.dto.CategoryResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /** 전체 목록. 경로순이라 부모가 항상 자식보다 먼저 온다 — 사이드바를 순서대로 그리면 된다. */
    @GetMapping
    public List<CategoryResponse> findAll() {
        return categoryService.findAll();
    }

    /** 전체 경로로 생성. 중간 단계가 없으면 함께 만들어진다. */
    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryCreateRequest request) {
        CategoryResponse created = categoryService.create(request.path(), request.color());
        return ResponseEntity
                .created(URI.create("/api/categories/" + created.id()))
                .body(created);
    }

    /** 이름 변경. 후손들의 경로도 함께 갱신된다. */
    @PatchMapping("/{id}/name")
    public CategoryResponse rename(@PathVariable Long id, @Valid @RequestBody CategoryRenameRequest request) {
        return categoryService.rename(id, request.name());
    }

    /** 색상·정렬순서·접힘·보관 변경. 넘긴 항목만 반영된다. */
    @PatchMapping("/{id}/appearance")
    public CategoryResponse updateAppearance(@PathVariable Long id,
                                             @Valid @RequestBody CategoryAppearanceRequest request) {
        return categoryService.updateAppearance(id, request);
    }

    /** 삭제. 하위가 있으면 400, 이 분류를 쓰던 일정은 미분류로 남는다. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
