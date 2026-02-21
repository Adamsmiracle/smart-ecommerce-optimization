package com.miracle.smart_ecommerce_jpa.exception;

/**
 * Thrown to indicate a client-side bad request.
 */
public class BadRequestException extends RuntimeException {
    public BadRequestException() { super(); }
    public BadRequestException(String message) { super(message); }
    public BadRequestException(String message, Throwable cause) { super(message, cause); }
}

