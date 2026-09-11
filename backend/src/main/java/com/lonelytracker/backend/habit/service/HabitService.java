package com.lonelytracker.backend.habit.service;

import com.lonelytracker.backend.common.exception.NotFoundException;
import com.lonelytracker.backend.habit.dto.HabitCreateRequest;
import com.lonelytracker.backend.habit.dto.HabitLogRequest;
import com.lonelytracker.backend.habit.dto.HabitResponse;
import com.lonelytracker.backend.habit.dto.HabitUpdateRequest;
import com.lonelytracker.backend.habit.entity.HabitEntity;
import com.lonelytracker.backend.habit.entity.HabitLogEntity;
import com.lonelytracker.backend.habit.repository.HabitLogRepository;
import com.lonelytracker.backend.habit.repository.HabitRepository;
import com.lonelytracker.backend.user.service.UserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 습관과 그 일지를 다룬다.
 * 일정과 섞지 않는다. 두 세계가 같은 테이블을 쓰지 않아 서로의 조회를 건드릴 일이 없다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HabitService {

    /** 일지가 한 번에 보여주는 날 수. 오늘을 끝으로 뒤로 센다 */
    private static final int WINDOW_DAYS = 14;

    /**
     * 갈래별로 묶기 전의 순서.
     * 사용자가 세운 자리를 지키고, 아직 정한 적이 없으면 만든 순서로 남는다.
     */
    private static final Comparator<HabitEntity> HABIT_ORDER =
            Comparator.comparingInt(HabitEntity::getDisplayOrder)
                    .thenComparing(HabitEntity::getId);

    private final HabitRepository habitRepository;
    private final HabitLogRepository logRepository;
    private final UserProvider currentUserProvider;

    /**
     * 습관 목록과 최근 기록.
     *
     * @param from null이면 오늘로부터 2주 전
     * @param to   null이면 오늘
     */
    public List<HabitResponse> findAll(LocalDate from, LocalDate to) {
        Long userId = currentUserProvider.get().getId();

        LocalDate windowTo = (to != null) ? to : LocalDate.now();
        LocalDate windowFrom = (from != null) ? from : windowTo.minusDays(WINDOW_DAYS - 1L);

        List<HabitEntity> habits = habitRepository.findAllOf(userId);
        if (habits.isEmpty()) {
            return List.of();
        }

        Map<Long, List<LocalDate>> doneDates = logRepository
                .findInRange(habits.stream().map(HabitEntity::getId).toList(),
                        windowFrom, windowTo)
                .stream()
                .collect(Collectors.groupingBy(
                        log -> log.getHabit().getId(),
                        Collectors.mapping(HabitLogEntity::getOnDate,
                                Collectors.toList())));

        return habits.stream()
                .sorted(HABIT_ORDER)
                .map(h -> HabitResponse.from(h,
                        doneDates.getOrDefault(h.getId(), List.of()).stream().sorted().toList()))
                .toList();
    }

    @Transactional
    public HabitResponse create(HabitCreateRequest request) {
        HabitEntity habit = habitRepository.save(HabitEntity.builder()
                .user(currentUserProvider.get())
                .title(request.title().strip())
                .category(request.category())
                .twoMinuteAction(request.twoMinuteAction())
                .build());

        return HabitResponse.from(habit, List.of());
    }

    @Transactional
    public HabitResponse update(Long id, HabitUpdateRequest request) {
        HabitEntity habit = getOwnedOrThrow(id);
        habit.update(request.title(), request.category(), request.twoMinuteAction());
        return HabitResponse.from(habitRepository.saveAndFlush(habit), List.of());
    }

    /**
     * 그만두거나 다시 시작한다.
     * 지우지 않는 이유는 지난 기록이 함께 사라지기 때문이다.
     */
    @Transactional
    public HabitResponse changeArchived(Long id, boolean archived) {
        HabitEntity habit = getOwnedOrThrow(id);
        habit.changeArchived(archived);
        return HabitResponse.from(habitRepository.saveAndFlush(habit), List.of());
    }

    /** 습관과 그 기록을 통째로 지운다 */
    @Transactional
    public void delete(Long id) {
        habitRepository.delete(getOwnedOrThrow(id));
    }

    /**
     * 그날 해냈는지 표시한다.
     * 줄이 있으면 해낸 것이라, 되돌리면 그 줄을 지운다.
     */
    @Transactional
    public void log(Long id, LocalDate onDate, HabitLogRequest request) {
        HabitEntity habit = getOwnedOrThrow(id);

        HabitLogEntity existing = logRepository
                .findByHabitIdAndOnDate(habit.getId(), onDate)
                .orElse(null);

        if (!Boolean.TRUE.equals(request.done())) {
            if (existing != null) {
                logRepository.delete(existing);
            }
            return;
        }

        if (existing != null) {
            existing.changeNote(request.note());
            logRepository.saveAndFlush(existing);
            return;
        }

        logRepository.save(HabitLogEntity.builder()
                .habit(habit)
                .onDate(onDate)
                .note(request.note())
                .build());
    }

    /** 남의 습관은 없는 것으로 취급한다. 400을 내면 그 습관의 존재가 새어 나간다 */
    private HabitEntity getOwnedOrThrow(Long id) {
        return habitRepository.findOwned(id, currentUserProvider.get().getId())
                .orElseThrow(() -> new NotFoundException("습관을 찾을 수 없습니다"));
    }
}
