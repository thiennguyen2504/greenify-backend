package com.webdev.greenify.common.exception;

import org.springframework.http.HttpStatus;

public class VoucherOutOfStockException extends AppException {
    public VoucherOutOfStockException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}