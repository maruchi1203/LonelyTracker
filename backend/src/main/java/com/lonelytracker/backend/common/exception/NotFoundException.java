package com.lonelytracker.backend.common.exception;

/**
 * 찾는 것이 없다. 404로 나간다.
 * 대상별 예외들이 이걸 상속한다.
 */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) {
        super(message);
    }
}
