package com.miracle.smart_ecommerce_jpa.aspects;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

/**
 * Aspect for monitoring and logging transaction execution.
 * Provides insights into transaction performance and rollback behavior.
 */
@Aspect
@Component
@Slf4j
public class TransactionMonitoringAspect {

    @Around("@annotation(org.springframework.transaction.annotation.Transactional) || @annotation(com.miracle.smart_ecommerce_jpa.annotation.CustomTransactional)")
    public Object monitorTransaction(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        long startTime = System.currentTimeMillis();
        boolean wasRolledBack = false;
        Exception exception = null;
        
        try {
            log.debug("Starting transaction for {}.{}", className, methodName);
            Object result = joinPoint.proceed();
            
            // Check if transaction was rolled back
            wasRolledBack = TransactionAspectSupport.currentTransactionStatus().isRollbackOnly();
            
            long duration = System.currentTimeMillis() - startTime;
            
            if (wasRolledBack) {
                log.warn("Transaction rolled back for {}.{} after {}ms", className, methodName, duration);
            } else {
                log.debug("Transaction committed for {}.{} in {}ms", className, methodName, duration);
            }
            
            return result;
            
        } catch (Exception e) {
            exception = e;
            wasRolledBack = TransactionAspectSupport.currentTransactionStatus().isRollbackOnly();
            
            long duration = System.currentTimeMillis() - startTime;
            log.error("Transaction failed for {}.{} after {}ms - Exception: {}", 
                className, methodName, duration, e.getMessage());
            
            throw e;
        }
    }
}
