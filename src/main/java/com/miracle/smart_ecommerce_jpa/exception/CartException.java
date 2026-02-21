package com.miracle.smart_ecommerce_jpa.exception;

/**
 * Exception thrown when cart operations fail.
 */
public class CartException extends RuntimeException {

    public CartException(String message) {
        super(message);
    }

    public CartException(String message, Throwable cause) {
        super(message, cause);
    }

    public static CartException forEmptyCart() {
        return new CartException("Cart is empty");
    }

    public static CartException forInvalidItem(String productId) {
        return new CartException("Invalid cart item: " + productId);
    }

    public static CartException forItemNotFound(String itemId) {
        return new CartException("Cart item not found: " + itemId);
    }

    public static CartException forInvalidQuantity(int quantity) {
        return new CartException("Invalid quantity: " + quantity);
    }
}

