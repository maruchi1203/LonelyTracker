package com.lonelytracker.backend.common.exception;

/**
 * AI 응답을 일정으로 읽지 못했다. 400으로 나간다.
 * 재시도해도 같은 결과이므로 직접 입력을 권한다.
 */
public class AiParseException extends RuntimeException {

    public AiParseException(String message) {
        super(message);
    }

    public AiParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
