package com.lonelytracker.backend.user;

import com.lonelytracker.backend.common.NotFoundException;

public class UserNotFoundException extends NotFoundException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
