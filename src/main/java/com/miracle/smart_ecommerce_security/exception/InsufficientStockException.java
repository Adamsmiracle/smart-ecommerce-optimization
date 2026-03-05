package com.miracle.smart_ecommerce_security.exception;

/**
 * Exception thrown when there is insufficient stock for a product.
 * Provides detailed information about the stock shortage for better error handling.
 */
public class InsufficientStockException extends RuntimeException {
    
    private final String productId;
    private final String productName;
    private final int available;
    private final int requested;
    private final String operation;
    private final String orderId;

    public InsufficientStockException(String productId, String productName, int available, int requested) {
        this(productId, productName, available, requested, null, null);
    }

    public InsufficientStockException(String productId, String productName, int available, int requested, String operation, String orderId) {
        super(String.format("Insufficient stock for product %s (%s): available=%d, requested=%d%s%s", 
            productName, productId, available, requested,
            operation != null ? " during " + operation : "",
            orderId != null ? " for order " + orderId : ""));
        this.productId = productId;
        this.productName = productName;
        this.available = available;
        this.requested = requested;
        this.operation = operation;
        this.orderId = orderId;
    }

    // Static factory methods for common scenarios
    public static InsufficientStockException forProduct(String productId, String productName, int available, int requested) {
        return new InsufficientStockException(productId, productName, available, requested);
    }

    public static InsufficientStockException forOrderItem(String productId, String productName, int available, int requested, String orderId) {
        return new InsufficientStockException(productId, productName, available, requested, "order_item_addition", orderId);
    }

    public static InsufficientStockException forCartUpdate(String productId, String productName, int available, int requested) {
        return new InsufficientStockException(productId, productName, available, requested, "cart_update", null);
    }

    public static InsufficientStockException forOrderCreation(String productId, String productName, int available, int requested, String orderId) {
        return new InsufficientStockException(productId, productName, available, requested, "order_creation", orderId);
    }

    public static InsufficientStockException forStockAdjustment(String productId, String productName, int available, int requested) {
        return new InsufficientStockException(productId, productName, available, requested, "stock_adjustment", null);
    }

    public static InsufficientStockException forBulkOperation(String productId, String productName, int available, int requested, int totalRequested) {
        return new InsufficientStockException(productId, productName, available, totalRequested, "bulk_operation", null);
    }

    // Getters
    public String getProductId() { return productId; }
    public String getProductName() { return productName; }
    public int getAvailable() { return available; }
    public int getRequested() { return requested; }
    public String getOperation() { return operation; }
    public String getOrderId() { return orderId; }

    public int getShortfall() { return Math.max(0, requested - available); }
    public boolean isCompletelyOutOfStock() { return available == 0; }
    public boolean hasPartialStock() { return available > 0 && available < requested; }

    /**
     * Returns a user-friendly message suggesting alternatives
     */
    public String getSuggestionMessage() {
        if (isCompletelyOutOfStock()) {
            return String.format("Product '%s' is currently out of stock. Please check back later or contact us for restocking information.", productName);
        } else if (hasPartialStock()) {
            return String.format("Only %d units of '%s' are available. You can add the available quantity to your cart or wait for restocking.", available, productName);
        } else {
            return String.format("Insufficient stock for '%s'. Available: %d, Requested: %d", productName, available, requested);
        }
    }

    /**
     * Returns inventory status information
     */
    public InventoryStatus getInventoryStatus() {
        return new InventoryStatus(productId, productName, available, requested, getShortfall(), isCompletelyOutOfStock(), hasPartialStock());
    }

    /**
     * Inventory status information
     */
    public static class InventoryStatus {
        private final String productId;
        private final String productName;
        private final int available;
        private final int requested;
        private final int shortfall;
        private final boolean completelyOutOfStock;
        private final boolean hasPartialStock;

        public InventoryStatus(String productId, String productName, int available, int requested, int shortfall, boolean completelyOutOfStock, boolean hasPartialStock) {
            this.productId = productId;
            this.productName = productName;
            this.available = available;
            this.requested = requested;
            this.shortfall = shortfall;
            this.completelyOutOfStock = completelyOutOfStock;
            this.hasPartialStock = hasPartialStock;
        }

        // Getters
        public String getProductId() { return productId; }
        public String getProductName() { return productName; }
        public int getAvailable() { return available; }
        public int getRequested() { return requested; }
        public int getShortfall() { return shortfall; }
        public boolean isCompletelyOutOfStock() { return completelyOutOfStock; }
        public boolean hasPartialStock() { return hasPartialStock; }
    }
}

