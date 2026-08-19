package com.lonelytracker.backend.user;

import com.lonelytracker.backend.common.NotFoundException;

public class CategoryNotFoundException extends NotFoundException {
    public CategoryNotFoundException(String message) {
        super(message);
    }
}
