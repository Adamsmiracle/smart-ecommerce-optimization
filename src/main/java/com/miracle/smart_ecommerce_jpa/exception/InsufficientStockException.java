package com.miracle.smart_ecommerce_jpa.exception;


public class InsufficientStockException extends RuntimeException {
    private final String productName;
    private final int available;
    private final int requested;

    public InsufficientStockException(String productName, int available, int requested) {
        super("Insufficient stock for product " + productName + ": available=" + available + ", requested=" + requested);
        this.productName = productName;
        this.available = available;
        this.requested = requested;
    }

    public String getProductName() {
        return productName;
    }

    public int getAvailable() {
        return available;
    }

    public int getRequested() {
        return requested;
    }
}

