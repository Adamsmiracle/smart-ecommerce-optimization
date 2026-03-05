package com.miracle.smart_ecommerce_security.exception;

/**
 * Exception thrown when an entity is in an invalid state for the requested operation.
 * This is commonly used for business logic validation that depends on entity state.
 */
public class EntityStateException extends RuntimeException {

    private final String entityType;
    private final String entityId;
    private final String currentState;
    private final String requiredState;

    public EntityStateException(String entityType, String entityId, String currentState, String requiredState) {
        super(String.format("Entity %s with ID %s is in state '%s' but operation requires state '%s'", 
            entityType, entityId, currentState, requiredState));
        this.entityType = entityType;
        this.entityId = entityId;
        this.currentState = currentState;
        this.requiredState = requiredState;
    }

    public EntityStateException(String entityType, String entityId, String currentState, String requiredState, Throwable cause) {
        super(String.format("Entity %s with ID %s is in state '%s' but operation requires state '%s'", 
            entityType, entityId, currentState, requiredState), cause);
        this.entityType = entityType;
        this.entityId = entityId;
        this.currentState = currentState;
        this.requiredState = requiredState;
    }

    // Static factory methods for common scenarios
    public static EntityStateException invalidOrderState(String orderId, String currentState, String requiredState) {
        return new EntityStateException("Order", orderId, currentState, requiredState);
    }

    public static EntityStateException invalidProductState(String productId, String currentState, String requiredState) {
        return new EntityStateException("Product", productId, currentState, requiredState);
    }

    public static EntityStateException invalidUserState(String userId, String currentState, String requiredState) {
        return new EntityStateException("User", userId, currentState, requiredState);
    }

    public static EntityStateException invalidCartState(String cartId, String currentState, String requiredState) {
        return new EntityStateException("Cart", cartId, currentState, requiredState);
    }

    public static EntityStateException entityNotFound(String entityType, String entityId) {
        return new EntityStateException(entityType, entityId, "NOT_FOUND", "EXISTS");
    }

    public static EntityStateException entityAlreadyDeleted(String entityType, String entityId) {
        return new EntityStateException(entityType, entityId, "DELETED", "ACTIVE");
    }

    public static EntityStateException entityNotActive(String entityType, String entityId) {
        return new EntityStateException(entityType, entityId, "INACTIVE", "ACTIVE");
    }

    // Getters
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
    public String getCurrentState() { return currentState; }
    public String getRequiredState() { return requiredState; }
}
