package com.lonelytracker.backend.user.controller;

import com.lonelytracker.backend.user.dto.UserCategoryAppearanceRequest;
import com.lonelytracker.backend.user.dto.UserCategoryCreateRequest;
import com.lonelytracker.backend.user.dto.UserCategoryRenameRequest;
import com.lonelytracker.backend.user.dto.UserCategoryResponse;
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
import com.lonelytracker.backend.user.service.UserCategoryService;

/**
 * 현재 사용자가 고를 수 있는 카테고리 목록.
 * 일정에는 이름이 문자열로 기록되므로, 여기서 무엇을 바꾸든 기존 일정은 영향받지 않는다.
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class UserCategoryController {

    private final UserCategoryService userCategoryService;

    /** 표시 순서대로 반환한다. */
    @GetMapping
    public List<UserCategoryResponse> findAll() {
        return userCategoryService.findAll();
    }

    /** 목록에 새 카테고리를 추가한다. 같은 이름이 이미 있으면 400. */
    @PostMapping
    public ResponseEntity<UserCategoryResponse> create(@Valid @RequestBody UserCategoryCreateRequest request) {
        UserCategoryResponse created = userCategoryService.create(request.name(), request.color());
        return ResponseEntity
                .created(URI.create("/api/categories/" + created.id()))
                .body(created);
    }

    /** 이름 변경. 이미 기록된 일정의 분류 문자열은 바뀌지 않는다. */
    @PatchMapping("/{id}/name")
    public UserCategoryResponse rename(@PathVariable Long id, @Valid @RequestBody UserCategoryRenameRequest request) {
        return userCategoryService.rename(id, request.name());
    }

    /** 색상·정렬순서·보관 변경. 넘긴 항목만 반영된다. */
    @PatchMapping("/{id}/appearance")
    public UserCategoryResponse updateAppearance(@PathVariable Long id,
                                             @Valid @RequestBody UserCategoryAppearanceRequest request) {
        return userCategoryService.updateAppearance(id, request);
    }

    /** 목록에서 제거. 이 분류를 쓰던 일정은 그대로 남는다. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userCategoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
