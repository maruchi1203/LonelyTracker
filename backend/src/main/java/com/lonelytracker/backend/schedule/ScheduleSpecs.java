package com.lonelytracker.backend.schedule;

import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

/** 값이 있을 때만 조건을 붙인다. null이면 조건 자체를 만들지 않는다. */
final class ScheduleSpecs {

    private ScheduleSpecs() {
    }

    /** 소유자 조건. 항상 붙는다 — 남의 일정이 섞이면 안 된다. */
    static Specification<Schedule> ownedBy(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
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
     * 분류 이름이 일치하는 일정만. 계층이 없어져 하위 포함 조회가 사라졌고,
     * 이름을 문자열로 들고 있어 조인도 필요 없다.
     */
    static Specification<Schedule> hasCategory(String category) {
        return (root, query, cb) -> {
            if (category == null || category.isBlank()) {
                return null;
            }
            return cb.equal(root.get("category"), category.strip());
        };
    }
}
