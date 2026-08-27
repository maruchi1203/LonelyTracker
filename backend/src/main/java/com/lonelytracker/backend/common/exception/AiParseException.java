package com.lonelytracker.backend.common.exception;

/**
 * 응답은 받았지만 쓸 수 있는 일정이 아니다. 400 으로 나간다.
 * <p>
 * 형식이 맞아도 내용이 틀릴 수 있다. <b>LLM 응답은 사용자 입력과 같은 등급으로
 * 취급해서 검증한다.</b> 재시도해도 같은 결과이므로 사용자에게 직접 입력을 권한다.
 */
public class AiParseException extends RuntimeException {

    public AiParseException(String message) {
        super(message);
    }

    public AiParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
