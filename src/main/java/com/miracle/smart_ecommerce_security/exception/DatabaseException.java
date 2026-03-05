package com.miracle.smart_ecommerce_security.exception;

/**
 * Exception thrown for database-related issues including connection problems,
 * transaction failures, and data inconsistencies.
 */
public class DatabaseException extends RuntimeException {

    private final String operation;
    private final String entityType;
    private final String entityId;

    public DatabaseException(String message) {
        super(message);
        this.operation = null;
        this.entityType = null;
        this.entityId = null;
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
        this.operation = null;
        this.entityType = null;
        this.entityId = null;
    }

    public DatabaseException(String operation, String entityType, String entityId, String message, Throwable cause) {
        super(message, cause);
        this.operation = operation;
        this.entityType = entityType;
        this.entityId = entityId;
    }

    // Static factory methods for common scenarios
    public static DatabaseException connectionFailed(String operation, Throwable cause) {
        return new DatabaseException(operation, null, null, 
            "Database connection failed during operation: " + operation, cause);
    }

    public static DatabaseException transactionFailed(String operation, Throwable cause) {
        return new DatabaseException(operation, null, null, 
            "Transaction failed during operation: " + operation, cause);
    }

    public static DatabaseException dataInconsistency(String entityType, String entityId, String details) {
        return new DatabaseException("DATA_INCONSISTENCY", entityType, entityId, 
            "Data inconsistency detected for " + entityType + " with ID " + entityId + ": " + details, null);
    }

    public static DatabaseException constraintViolation(String constraint, String entityType, String details) {
        return new DatabaseException("CONSTRAINT_VIOLATION", entityType, null, 
            "Database constraint '" + constraint + "' violated for " + entityType + ": " + details, null);
    }

    public static DatabaseException deadlockDetected(String operation, String entityType, String entityId) {
        return new DatabaseException(operation, entityType, entityId, 
            "Database deadlock detected during " + operation + " for " + entityType + " " + entityId, null);
    }

    public static DatabaseException lockTimeout(String operation, String entityType, String entityId) {
        return new DatabaseException(operation, entityType, entityId, 
            "Lock timeout during " + operation + " for " + entityType + " " + entityId, null);
    }

    // Getters
    public String getOperation() { return operation; }
    public String getEntityType() { return entityType; }
    public String getEntityId() { return entityId; }
}
