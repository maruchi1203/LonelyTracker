package com.lonelytracker.backend.ai;

import com.lonelytracker.backend.common.exception.AiUnavailableException;

/**
 * 이번 AI 호출이 향할 곳을 정하는 계약
 */
public interface AiTargetResolver {

    /**
     * @throws AiUnavailableException 부를 곳이 없을 때
     */
    AiTarget resolve();
}
