package com.lonelytracker.backend.common.exception;


/**
 * 그 카테고리가 없거나 남의 것이다. 404로 나간다.
 */
public class CategoryNotFoundException extends NotFoundException {
    public CategoryNotFoundException(String message) {
        super(message);
    }
}
