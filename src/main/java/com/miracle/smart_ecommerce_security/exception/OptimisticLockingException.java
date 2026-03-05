package com.miracle.smart_ecommerce_security.exception;

/**
 * Exception thrown when optimistic locking fails due to concurrent modifications.
 * This occurs when multiple users try to update the same entity simultaneously.
 */
public class OptimisticLockingException extends RuntimeException {

    private final String entityType;
    private final String entityId;
    private final Long expectedVersion;
    private final Long actualVersion;

    public OptimisticLockingException(String entityType, String entityId, Long expectedVersion, Long actualVersion) {
        super(String.format("Optimistic locking failed for %s with ID %s. Expected version %d but found version %d. " +
            "The entity has been modified by another transaction.", entityType, entityId, expectedVersion, actualVersion));
        this.entityType = entityType;
        this.entityId = entityId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
    }

    public OptimisticLockingException(String entityType, String entityId, Long expectedVersion, Long actualVersion, Throwable cause) {
        super(String.format("Optimistic locking failed for %s with ID %s. Expected version %d but found version %d. " +
            "The entity has been modified by another transaction.", entityType, entityId, expectedVersion, actualVersion), cause);
        this.entityType = entityType;
        this.entityId = entityId;
        this.expectedVersion = expectedVersion;
        this.actualVersion = actualVersion;
    }

    // Static factory methods for common scenarios
    public static OptimisticLockingException productModified(String productId, Long expectedVersion, Long actualVersion) {
        return new OptimisticLockingException("Product", productId, expectedVersion, actualVersion);
    }

    public static OptimisticLockingException orderModified(String orderId, Long expectedVersion, Long actualVersion) {
        return new OptimisticLockingException("Order", orderId, expectedVersion, actualVersion);
    }

    public static OptimisticLockingException userModified(String userId, Long expectedVersion, Long actualVersion) {
        return new OptimisticLockingException("User", userId, expectedVersion, actualVersion);
    }

    public static OptimisticLockingException cartModified(String cartId, Long expectedVersion, Long actualVersion) {
        return new OptimisticLockingException("Cart", cartId, expectedVersion, actualVersion);
    }

    public static OptimisticLockingException addressModified(String addressId, Long expectedVersion, Long actualVersion) {
        return new OptimisticLockingException("Address", addressId, expectedVersion, actualVersion);
    }

    public static OptimisticLockingException categoryModified(String categoryId, Long expectedVersion, Long actualVersion) {
        return new OptimisticLockingException("Category", categoryId, expectedVersion, actualVersion);
    }

    // Getters
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public Long getExpectedVersion() { return expectedVersion; }
    public Long getActualVersion() { return actualVersion; }

    /**
     * Returns a suggested retry message for the client
     */
    public String getRetryMessage() {
        return String.format("The %s you were trying to modify has been changed by another user. " +
            "Please refresh your data and try again.", entityType.toLowerCase());
    }
}
