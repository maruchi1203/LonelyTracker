package com.lonelytracker.backend.schedule;

import com.lonelytracker.backend.schedule.repository.ScheduleRecurRepository;
import com.lonelytracker.backend.schedule.repository.ScheduleRepository;
import com.lonelytracker.backend.support.FixedClockConfig;
import com.lonelytracker.backend.support.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 멈춘 시계가 실제로 운영 코드까지 닿는지 본다.
 * <p>
 * 2026-10-02 에 리스트 테스트가 깨졌다. 시작일을 2026-10-01 로 적고 지금 할 회차도
 * 2026-10-01 을 기대했는데, {@code currentOccurrence} 가 {@code max(오늘, 시작일)} 부터
 * 세기 때문에 10-02 가 되자 밀렸다. 10-01 까지만 통과하던 테스트다.
 * <p>
 * 시계를 멈추면 날짜를 적어도 된다. 이 테스트가 그 장치의 증거다 —
 * 시계가 안 끼워지면 "오늘"이 흘러가 {@code occurrenceOn} 이 달라지고 여기서 걸린다.
 */
@AutoConfigureMockMvc
@Import(FixedClockConfig.class)
class FixedClockScheduleTest extends IntegrationTest {

    private static final String BASE = "/api/schedules";

    @Autowired
    MockMvc mvc;

    @Autowired
    ScheduleRepository scheduleRepository;

    @Autowired
    ScheduleRecurRepository recurRepository;

    @AfterEach
    void clean() {
        // 규칙이 일정을 참조한다. 일정을 먼저 지우면 외래 키가 걸린다
        recurRepository.deleteAll();
        scheduleRepository.deleteAll();
    }

    @Test
    @DisplayName("멈춘 시계를 운영 코드가 그대로 쓴다")
    void readsTheFrozenToday() throws Exception {
        String today = FixedClockConfig.TODAY.toString();

        mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"매일 운동\",\"startAt\":\"" + today
                                + "T07:00:00\",\"recurrence\":{\"freq\":\"DAILY\"}}"))
                .andExpect(status().isCreated());

        mvc.perform(get(BASE + "/list"))
                .andExpect(jsonPath("$[0].occurrenceOn").value(today));
    }

    @Test
    @DisplayName("날짜를 안 준 반복은 멈춘 오늘로 시작한다")
    void fillsTheFrozenTodayForARecurringWithoutAStart() throws Exception {
        // startOf() 가 시계를 쓰는지 본다. 정적 메서드였던 자리라 빠뜨리기 쉽다
        mvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"복근 운동\",\"recurrence\":{\"freq\":\"DAILY\"}}"))
                .andExpect(status().isCreated());

        mvc.perform(get(BASE + "/list"))
                .andExpect(jsonPath("$[0].startAt")
                        .value(FixedClockConfig.TODAY + "T00:00:00"))
                .andExpect(jsonPath("$[0].occurrenceOn")
                        .value(FixedClockConfig.TODAY.toString()));
    }
}
