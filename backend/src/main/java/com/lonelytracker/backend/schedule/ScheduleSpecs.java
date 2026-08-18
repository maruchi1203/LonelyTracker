package com.lonelytracker.backend.schedule;

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
}
