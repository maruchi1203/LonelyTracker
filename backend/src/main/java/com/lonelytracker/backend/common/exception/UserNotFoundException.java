package com.lonelytracker.backend.common.exception;


/**
 * 그 사용자가 없다. 404로 나간다.
 */
public class UserNotFoundException extends NotFoundException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
