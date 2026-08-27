package com.lonelytracker.backend.common.exception;

/**
 * 암호화 마스터 키가 없어 민감한 값을 저장·조회할 수 없다. 503 으로 나간다.
 * <p>
 * 사용자 잘못이 아니라 <b>서버 설정이 빠진 것</b>이다. 그냥 500 을 내면
 * 무엇이 빠졌는지 알 수 없어 진단이 안 된다.
 */
public class EncryptionNotConfiguredException extends RuntimeException {

    public EncryptionNotConfiguredException(String message) {
        super(message);
    }
}
