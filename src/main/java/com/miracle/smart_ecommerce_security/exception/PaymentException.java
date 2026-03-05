package com.miracle.smart_ecommerce_security.exception;

/**
 * Exception thrown when a payment operation fails.
 */
public class PaymentException extends RuntimeException {

    public PaymentException(String message) {
        super(message);
    }

    public PaymentException(String message, Throwable cause) {
        super(message, cause);
    }

    public static PaymentException forFailedTransaction(String transactionId) {
        return new PaymentException("Payment transaction failed: " + transactionId);
    }

    public static PaymentException forInsufficientFunds() {
        return new PaymentException("Insufficient funds for payment");
    }

    public static PaymentException forInvalidPaymentMethod() {
        return new PaymentException("Invalid payment method");
    }
}

