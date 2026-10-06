package com.lonelytracker.backend.common.exception;

/**
 * 요청은 멀쩡한데 지금 상태가 받아 주지 않을 때.
 * <p>
 * 보낸 값이 잘못된 것(400)과 가른다. 고쳐 보낼 데가 없고, 상태가 달라지면 같은 요청이
 * 그대로 통한다 — 갈래가 둘이 되면 아까 막힌 삭제가 된다.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
