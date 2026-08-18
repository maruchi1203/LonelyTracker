package com.lonelytracker.backend.schedule;

import com.lonelytracker.backend.category.Category;
import com.lonelytracker.backend.category.CategoryPath;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/** 값이 있을 때만 조건을 붙인다. null이면 조건 자체를 만들지 않는다. */
final class ScheduleSpecs {

    private ScheduleSpecs() {
    }

    static Specification<Schedule> startAtFrom(LocalDateTime from) {
        return (root, query, cb) ->
                from == null ? null : cb.greaterThanOrEqualTo(root.get("startAt"), from);
    }

    static Specification<Schedule> startAtTo(LocalDateTime to) {
        return (root, query, cb) ->
                to == null ? null : cb.lessThanOrEqualTo(root.get("startAt"), to);
    }

    static Specification<Schedule> hasStatus(ScheduleStatus status) {
        return (root, query, cb) ->
                status == null ? null : cb.equal(root.get("status"), status);
    }

    /**
     * 해당 카테고리와 그 하위 카테고리를 모두 포함한다.
     * {@code 능력} 으로 조회하면 {@code 능력}, {@code 능력\개발} 은 걸리고
     * {@code 능력강화} 처럼 이름만 비슷한 것은 걸리지 않는다(구분자를 붙여 비교하므로).
     */
    static Specification<Schedule> inCategory(String rawPath) {
        return (root, query, cb) -> {
            String path = CategoryPath.normalize(rawPath);
            if (path == null) {
                return null;
            }
            // INNER JOIN이면 미분류 일정이 자동으로 빠지는데, 그게 의도한 동작이다
            Join<Schedule, Category> category = root.join("category", JoinType.INNER);
            return cb.or(
                    cb.equal(category.get("path"), path),
                    cb.like(category.get("path"), CategoryPath.descendantPattern(path),
                            CategoryPath.LIKE_ESCAPE)
            );
        };
    }
}
