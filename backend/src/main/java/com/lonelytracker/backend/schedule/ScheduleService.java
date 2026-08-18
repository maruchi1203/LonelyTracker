package com.lonelytracker.backend.schedule;

import com.lonelytracker.backend.category.Category;
import com.lonelytracker.backend.category.CategoryService;
import com.lonelytracker.backend.common.NotFoundException;
import com.lonelytracker.backend.schedule.dto.ScheduleCreateRequest;
import com.lonelytracker.backend.schedule.dto.ScheduleResponse;
import com.lonelytracker.backend.schedule.dto.ScheduleUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final CategoryService categoryService;

    public List<ScheduleResponse> search(LocalDateTime from, LocalDateTime to,
                                        ScheduleStatus status, String category) {
        Specification<Schedule> spec = Specification
                .allOf(ScheduleSpecs.startAtFrom(from))
                .and(ScheduleSpecs.startAtTo(to))
                .and(ScheduleSpecs.hasStatus(status))
                .and(ScheduleSpecs.inCategory(category));

        return scheduleRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "startAt")).stream()
                .map(ScheduleResponse::from)
                .toList();
    }

    public ScheduleResponse findById(Long id) {
        return ScheduleResponse.from(getOrThrow(id));
    }

    @Transactional
    public ScheduleResponse create(ScheduleCreateRequest request) {
        validatePeriod(request.startAt(), request.endAt());

        Schedule schedule = Schedule.builder()
                .title(request.title())
                .description(request.description())
                .startAt(request.startAt())
                .endAt(request.endAt())
                .allDay(Boolean.TRUE.equals(request.allDay()))
                .category(categoryService.getOrCreate(request.categoryPath()))
                .build();

        return ScheduleResponse.from(scheduleRepository.save(schedule));
    }

    @Transactional
    public ScheduleResponse update(Long id, ScheduleUpdateRequest request) {
        validatePeriod(request.startAt(), request.endAt());

        Schedule schedule = getOrThrow(id);
        // save()를 부르지 않아도 트랜잭션이 끝날 때 변경 감지로 UPDATE가 나간다
        schedule.update(
                request.title(),
                request.description(),
                request.startAt(),
                request.endAt(),
                Boolean.TRUE.equals(request.allDay()),
                categoryService.getOrCreate(request.categoryPath())
        );
        // 는 flush 시점에 채워진다. 응답에 갱신된 updatedAt을 담으려면 먼저 flush.
        return ScheduleResponse.from(scheduleRepository.saveAndFlush(schedule));
    }

    @Transactional
    public ScheduleResponse changeStatus(Long id, ScheduleStatus status) {
        Schedule schedule = getOrThrow(id);
        schedule.changeStatus(status);
        return ScheduleResponse.from(scheduleRepository.saveAndFlush(schedule));
    }

    @Transactional
    public void delete(Long id) {
        scheduleRepository.delete(getOrThrow(id));
    }

    private Schedule getOrThrow(Long id) {
        return scheduleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("일정을 찾을 수 없습니다. id=" + id));
    }

    private void validatePeriod(LocalDateTime startAt, LocalDateTime endAt) {
        if (endAt != null && endAt.isBefore(startAt)) {
            throw new IllegalArgumentException("endAt은 startAt보다 이를 수 없습니다");
        }
    }
}
