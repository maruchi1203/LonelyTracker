package com.lonelytracker.backend.common.exception;

/**
 * 이번 달 토큰 한도를 다 썼다.
 * 잘못된 요청이 아니라 사용자가 정해 둔 정책이라, 부르는 쪽이 안내 문구로 바꿔 내보낸다.
 */
public class AiLimitExceededException extends RuntimeException {

    public AiLimitExceededException(String message) {
        super(message);
    }
}
