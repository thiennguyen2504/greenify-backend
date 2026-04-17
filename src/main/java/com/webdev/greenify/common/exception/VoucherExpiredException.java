package com.webdev.greenify.common.exception;

import org.springframework.http.HttpStatus;

public class VoucherExpiredException extends AppException {
    public VoucherExpiredException(String message) {
        super(message, HttpStatus.BAD_REQUEST);
    }
}