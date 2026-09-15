package com.lonelytracker.backend.schedule;

import com.lonelytracker.backend.ai.AiTarget;
import com.lonelytracker.backend.ai.AiTargetResolver;
import com.lonelytracker.backend.ai.AiUsage;
import com.lonelytracker.backend.ai.AiUsageRecorder;
import com.lonelytracker.backend.ai.ParseResult;
import com.lonelytracker.backend.ai.ParsedSchedule;
import com.lonelytracker.backend.ai.ScheduleParser;
import com.lonelytracker.backend.schedule.service.ScheduleParseService;
import com.lonelytracker.backend.schedule.service.ScheduleService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScheduleParseServiceTest {

    @Test
    void returnsDraftsEvenWhenUsageRecordingFails() {
        ScheduleParser parser = mock(ScheduleParser.class);
        ScheduleService scheduleService = mock(ScheduleService.class);
        AiTargetResolver targetResolver = mock(AiTargetResolver.class);
        AiUsageRecorder usageRecorder = mock(AiUsageRecorder.class);

        when(targetResolver.resolve()).thenReturn(new AiTarget("https://api.openai.com/v1", "m", "k"));
        when(scheduleService.findTagNames()).thenReturn(List.of());
        ParsedSchedule draft = new ParsedSchedule("운동", null, null, false, List.of(), null, null, List.of());
        when(parser.parse(any())).thenReturn(new ParseResult(List.of(draft), new AiUsage(1, 2)));
        doThrow(new IllegalStateException("db down")).when(usageRecorder).record(any(), any(), any());

        ScheduleParseService service =
                new ScheduleParseService(parser, scheduleService, targetResolver, usageRecorder);

        assertThat(service.parse("운동").stream().map(ParsedSchedule::title)).containsExactly("운동");
    }
}
