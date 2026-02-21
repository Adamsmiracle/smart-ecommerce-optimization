package com.miracle.smart_ecommerce_jpa.exception;

/**
 * Thrown when authentication/authorization fails.
 */
public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException() { super(); }
    public UnauthorizedException(String message) { super(message); }
    public UnauthorizedException(String message, Throwable cause) { super(message, cause); }
}

