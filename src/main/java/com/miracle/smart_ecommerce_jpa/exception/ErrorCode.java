package com.miracle.smart_ecommerce_jpa.exception;

/**
 * Application-level error codes for consistent API error handling.
 */
public enum ErrorCode {
    // Client errors (4xx)
    RESOURCE_NOT_FOUND,
    BAD_REQUEST,
    UNAUTHORIZED,
    FORBIDDEN,
    CONFLICT,
    VALIDATION_FAILED,
    DUPLICATE_RESOURCE,
    INSUFFICIENT_STOCK,

    // Server errors (5xx)
    INTERNAL_ERROR,
    DATA_INTEGRITY,
    DATABASE_ERROR,
    SERVICE_UNAVAILABLE,

    // Security errors
    AUTHENTICATION_FAILED,
    ACCESS_DENIED,
    TOKEN_EXPIRED,
    TOKEN_INVALID,

    // Business logic errors
    PAYMENT_FAILED,
    ORDER_PROCESSING_ERROR,
    CART_ERROR,
    INVENTORY_ERROR
}

