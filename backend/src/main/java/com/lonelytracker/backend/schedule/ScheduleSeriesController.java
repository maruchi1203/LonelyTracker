package com.lonelytracker.backend.schedule;

import com.lonelytracker.backend.schedule.dto.ScheduleSeriesCreateRequest;
import com.lonelytracker.backend.schedule.dto.ScheduleSeriesResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * 반복 규칙.
 * <p>
 * 회차는 이 컨트롤러의 하위 경로({@code /occurrences/{date}})로 다룬다.
 * 회차는 행이 아니므로 id 가 없고 {@code seriesId + onDate} 로 식별한다.
 */
@RestController
@RequestMapping("/api/series")
@RequiredArgsConstructor
public class ScheduleSeriesController {

    private final ScheduleSeriesService scheduleSeriesService;

    @PostMapping
    public ResponseEntity<ScheduleSeriesResponse> create(
            @Valid @RequestBody ScheduleSeriesCreateRequest request) {
        ScheduleSeriesResponse created = scheduleSeriesService.create(request);
        return ResponseEntity
                .created(URI.create("/api/series/" + created.seriesId()))
                .body(created);
    }
}
