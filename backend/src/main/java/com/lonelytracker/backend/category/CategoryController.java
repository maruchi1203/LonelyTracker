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

    // 전체 목록 호출
    @GetMapping
    public List<CategoryResponse> findAll() {
        return categoryService.findAll();
    }

    // 전체 경로로 생성
    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryCreateRequest request) {
        CategoryResponse createdCategory = categoryService.create(request.path(), request.color());
        return ResponseEntity
                .created(URI.create("/api/categories/" + createdCategory.id()))
                .body(createdCategory);
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

    /** 삭제. 하위 카테고리도 함께 지우고, 딸린 일정은 부모 카테고리로 옮긴다. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.deleteWithAllDescendants(id);
        return ResponseEntity.noContent().build();
    }
}
