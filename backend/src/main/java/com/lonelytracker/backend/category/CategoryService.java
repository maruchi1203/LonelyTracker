package com.lonelytracker.backend.category;

import com.lonelytracker.backend.category.dto.CategoryAppearanceRequest;
import com.lonelytracker.backend.category.dto.CategoryResponse;
import com.lonelytracker.backend.schedule.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ScheduleRepository scheduleRepository;

    public List<CategoryResponse> findAll() {
        return categoryRepository.findAllByOrderByPathAsc().stream()
                .map(CategoryResponse::from)
                .toList();
    }

    /**
     * 경로로 카테고리를 찾고, 없으면 만든다. 중간 단계가 비어 있으면 함께 만든다.
     * 예를 들어 {@code 능력\개발\SpringBoot} 하나로 최대 3개가 생성될 수 있다.
     * <p>
     * 일정 저장 시 분류를 자유 입력으로 받기 위해 필요하다.
     */
    @Transactional
    public Category getOrCreate(String rawPath) {
        String path = CategoryPath.normalize(rawPath);
        if (path == null) {
            return null;
        }
        return categoryRepository.findByPath(path)
                .orElseGet(() -> {
                    // 부모를 먼저 확보해야 parent_id를 채울 수 있다
                    Category parent = getOrCreate(CategoryPath.parentOf(path));
                    return categoryRepository.save(Category.builder()
                            .name(CategoryPath.nameOf(path))
                            .path(path)
                            .parent(parent)
                            .build());
                });
    }

    @Transactional
    public CategoryResponse create(String rawPath, String color) {
        String path = CategoryPath.normalize(rawPath);
        if (path == null) {
            throw new IllegalArgumentException("path가 비어 있습니다");
        }
        if (categoryRepository.existsByPath(path)) {
            throw new IllegalArgumentException("이미 존재하는 카테고리입니다: " + path);
        }
        Category created = getOrCreate(path);
        created.updateAppearance(color, null, null, null);
        return CategoryResponse.from(created);
    }

    /**
     * 마지막 세그먼트만 바꾼다. path를 중복 보관하는 대가로,
     * 자신뿐 아니라 모든 후손의 path도 함께 갱신해야 한다.
     */
    @Transactional
    public CategoryResponse rename(Long id, String newName) {
        Category category = checkAndGetId(id);

        String name = newName.strip();
        if (name.contains(CategoryPath.SEPARATOR)) {
            throw new IllegalArgumentException("이름에는 구분자(\\)를 쓸 수 없습니다. 위치 이동은 별도 기능입니다");
        }

        String oldPath = category.getPath();
        String parentPath = CategoryPath.parentOf(oldPath);
        String newPath = parentPath == null ? name : parentPath + CategoryPath.SEPARATOR + name;

        if (newPath.equals(oldPath)) {
            return CategoryResponse.from(category);
        }
        if (categoryRepository.existsByPath(newPath)) {
            throw new IllegalArgumentException("같은 위치에 이미 존재하는 이름입니다: " + newPath);
        }

        // 하위 항목을 먼저 옮김
        for (Category descendant : categoryRepository.findDescendants(CategoryPath.descendantPattern(oldPath))) {
            String moved = newPath + descendant.getPath().substring(oldPath.length());
            descendant.rename(descendant.getName(), moved);
        }

        category.rename(name, newPath);
        return CategoryResponse.from(category);
    }

    // 구별용 색상이나 표시순서를 변경함
    @Transactional
    public CategoryResponse updateAppearance(Long id, CategoryAppearanceRequest request) {
        Category category = checkAndGetId(id);
        category.updateAppearance(request.color(), request.displayOrder(),
                request.collapsed(), request.archived());
        return CategoryResponse.from(category);
    }

    /**
     * 하위 카테고리가 있으면 전부 삭제한다.
     */
    @Transactional
    public void deleteWithAllDescendants(Long id) {
        Category target = checkAndGetId(id);
        Category newCategory = target.getParent();          // null이면 미분류

        List<Category> doomed = new ArrayList<>();
        doomed.add(target);
        doomed.addAll(categoryRepository.findDescendants(
                CategoryPath.descendantPattern(target.getPath())));   // ① 후손 수집

        List<Long> doomedIds = doomed.stream().map(Category::getId).toList();

        scheduleRepository.moveCategory(doomedIds, newCategory);      // ② 일정 떼어내기
        categoryRepository.deleteAllByIdInBatch(doomedIds);           // ③ 한 번에 삭제
    }

    // id가 없다면 Throw하는 로직
    private Category checkAndGetId(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException("카테고리를 찾을 수 없습니다. id=" + id));
    }
}
