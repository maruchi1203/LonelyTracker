package com.lonelytracker.backend.common.exception;

/**
 * 암호화 마스터 키가 없어 민감한 값을 저장·조회할 수 없다. 503으로 나간다.
 * 사용자 잘못이 아니라 서버 설정이 빠진 것이다.
 */
public class EncryptionNotConfiguredException extends RuntimeException {

    public EncryptionNotConfiguredException(String message) {
        super(message);
    }
}
