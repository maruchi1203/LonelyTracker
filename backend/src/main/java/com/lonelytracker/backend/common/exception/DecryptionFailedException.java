package com.lonelytracker.backend.common.exception;

/**
 * 저장된 값을 복호화하지 못했다. 503 으로 나간다.
 * <p>
 * 대개 <b>마스터 키가 바뀐 것</b>이다. 값 자체가 조작된 경우도 여기로 온다 —
 * AES-GCM 이 무결성까지 검증하기 때문이다.
 * <p>
 * 사용자 잘못이 아니고, 무엇을 해야 하는지(키를 다시 등록) 알려줘야 하므로
 * 아무 정보 없는 500 이 되면 안 된다.
 */
public class DecryptionFailedException extends RuntimeException {

    public DecryptionFailedException(String message) {
        super(message);
    }
}
