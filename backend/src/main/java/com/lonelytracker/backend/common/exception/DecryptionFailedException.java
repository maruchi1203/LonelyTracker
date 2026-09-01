package com.lonelytracker.backend.common.exception;

/**
 * 저장된 값을 복호화하지 못했다. 503으로 나간다.
 * 대개 마스터 키가 바뀐 것이고, 값이 조작된 경우도 여기로 온다.
 */
public class DecryptionFailedException extends RuntimeException {

    public DecryptionFailedException(String message) {
        super(message);
    }
}
