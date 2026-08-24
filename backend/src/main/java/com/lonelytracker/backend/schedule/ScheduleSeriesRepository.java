package com.lonelytracker.backend.schedule;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleSeriesRepository extends JpaRepository<ScheduleSeries, Long> {
}
