package com.webdev.greenify.common.exception;

import org.springframework.http.HttpStatus;

public class EmailSendingException extends AppException {
    public EmailSendingException(String message, Throwable cause) {
        super(message, HttpStatus.INTERNAL_SERVER_ERROR);
        this.initCause(cause);
    }
}
