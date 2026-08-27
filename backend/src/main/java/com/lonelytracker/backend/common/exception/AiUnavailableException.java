package com.lonelytracker.backend.common.exception;

/**
 * AI 기능을 지금 쓸 수 없다. 503 으로 나간다.
 * <p>
 * 키가 없거나, 타임아웃이거나, 제공자가 장애인 경우다.
 * <b>사용자 잘못이 아니므로 4xx 가 아니다.</b> 직접 입력으로 유도한다.
 */
public class AiUnavailableException extends RuntimeException {

    public AiUnavailableException(String message) {
        super(message);
    }

    public AiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
