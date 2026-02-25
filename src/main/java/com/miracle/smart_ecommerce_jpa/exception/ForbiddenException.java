package com.miracle.smart_ecommerce_jpa.exception;

/**
 * Thrown when an authenticated user does not have sufficient privileges to access a resource.
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException() { super(); }
    public ForbiddenException(String message) { super(message); }
    public ForbiddenException(String message, Throwable cause) { super(message, cause); }
}

