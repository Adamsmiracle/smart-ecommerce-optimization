package com.miracle.smart_ecommerce_jpa.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.function.Supplier;

/**
 * Utility class for advanced transaction management.
 * Provides programmatic transaction control for complex business scenarios.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TransactionManagerUtil {

    private final PlatformTransactionManager transactionManager;

    /**
     * Execute operation within a new transaction.
     * 
     * @param operation the operation to execute
     * @param <T> return type
     * @return result of the operation
     */
    public <T> T executeInNewTransaction(Supplier<T> operation) {
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        definition.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        definition.setTimeout(30);
        
        TransactionStatus status = transactionManager.getTransaction(definition);
        
        try {
            log.debug("Starting new transaction for operation");
            T result = operation.get();
            transactionManager.commit(status);
            log.debug("Transaction committed successfully");
            return result;
        } catch (Exception e) {
            log.error("Exception in new transaction, rolling back: {}", e.getMessage());
            transactionManager.rollback(status);
            throw e;
        }
    }

    /**
     * Execute operation within a read-only transaction.
     * 
     * @param operation the operation to execute
     * @param <T> return type
     * @return result of the operation
     */
    public <T> T executeInReadOnlyTransaction(Supplier<T> operation) {
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        definition.setReadOnly(true);
        definition.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        definition.setTimeout(30);
        
        TransactionStatus status = transactionManager.getTransaction(definition);
        
        try {
            log.debug("Starting read-only transaction for operation");
            T result = operation.get();
            transactionManager.commit(status);
            log.debug("Read-only transaction completed successfully");
            return result;
        } catch (Exception e) {
            log.error("Exception in read-only transaction, rolling back: {}", e.getMessage());
            transactionManager.rollback(status);
            throw e;
        }
    }

    /**
     * Execute multiple operations in separate transactions.
     * Useful for batch processing where individual failures shouldn't affect others.
     * 
     * @param operations array of operations to execute
     * @param <T> return type
     * @return array of results
     */
    @SuppressWarnings("unchecked")
    public <T> T[] executeInSeparateTransactions(Supplier<T>... operations) {
        if (operations == null || operations.length == 0) {
            return (T[]) new Object[0];
        }
        
        Object[] results = new Object[operations.length];
        
        for (int i = 0; i < operations.length; i++) {
            try {
                results[i] = executeInNewTransaction(operations[i]);
            } catch (Exception e) {
                log.error("Operation {} failed in separate transaction: {}", i, e.getMessage());
                throw e;
            }
        }
        
        return (T[]) results;
    }

    /**
     * Execute operation with custom timeout.
     * 
     * @param operation the operation to execute
     * @param timeout timeout in seconds
     * @param <T> return type
     * @return result of the operation
     */
    public <T> T executeWithTimeout(Supplier<T> operation, int timeout) {
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        definition.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        definition.setTimeout(timeout);
        
        TransactionStatus status = transactionManager.getTransaction(definition);
        
        try {
            log.debug("Starting transaction with timeout {} seconds", timeout);
            T result = operation.get();
            transactionManager.commit(status);
            log.debug("Transaction with timeout completed successfully");
            return result;
        } catch (Exception e) {
            log.error("Exception in timed transaction, rolling back: {}", e.getMessage());
            transactionManager.rollback(status);
            throw e;
        }
    }

    /**
     * Check if currently in a transaction.
     * 
     * @return true if in active transaction
     */
    public boolean isTransactionActive() {
        try {
            return transactionManager.getTransaction(null) != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Mark current transaction for rollback only.
     */
    public void setRollbackOnly() {
        try {
            TransactionStatus status = transactionManager.getTransaction(null);
            if (status != null && !status.isCompleted()) {
                status.setRollbackOnly();
                log.debug("Current transaction marked for rollback only");
            }
        } catch (Exception e) {
            log.error("Failed to mark transaction for rollback only: {}", e.getMessage());
        }
    }
}
