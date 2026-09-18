package com.lonelytracker.backend.user.controller;

import com.lonelytracker.backend.user.dto.AiUsageSummaryResponse;
import com.lonelytracker.backend.user.service.AiUsageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** AI 사용량. 제공자별로 이번 주와 이번 달을 모은다 */
@RestController
@RequestMapping("/api/users/me/ai-usage")
@RequiredArgsConstructor
public class AiUsageController {

    private final AiUsageService usageService;

    @GetMapping
    public AiUsageSummaryResponse summary() {
        return usageService.summary();
    }
}
