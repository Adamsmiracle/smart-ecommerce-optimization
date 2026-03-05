package com.miracle.smart_ecommerce_security.exception;

/**
 * Exception thrown when order processing fails.
 */
public class OrderProcessingException extends RuntimeException {

    public OrderProcessingException(String message) {
        super(message);
    }

    public OrderProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public static OrderProcessingException forInvalidOrder(String reason) {
        return new OrderProcessingException("Invalid order: " + reason);
    }

    public static OrderProcessingException forCancellationFailure(String orderNumber) {
        return new OrderProcessingException("Failed to cancel order: " + orderNumber);
    }

    public static OrderProcessingException forStatusUpdateFailure(String orderNumber, String status) {
        return new OrderProcessingException("Failed to update order " + orderNumber + " to status: " + status);
    }
}

