package com.webdev.greenify.common.exception;

import org.springframework.http.HttpStatus;

public class TokenException extends AppException {
    public TokenException(String message) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public TokenException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
        this.initCause(cause);
    }
}
