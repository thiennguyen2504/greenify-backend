package com.webdev.greenify.common.exception;

import org.springframework.http.HttpStatus;

public class InsufficientPointsException extends AppException {
    public InsufficientPointsException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}