package com.lonelytracker.backend.common.exception;

/**
 * AI를 지금 쓸 수 없다. 503으로 나간다.
 * 키가 없거나, 타임아웃이거나, 제공자가 장애인 경우다.
 */
public class AiUnavailableException extends RuntimeException {

    public AiUnavailableException(String message) {
        super(message);
    }

    public AiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
