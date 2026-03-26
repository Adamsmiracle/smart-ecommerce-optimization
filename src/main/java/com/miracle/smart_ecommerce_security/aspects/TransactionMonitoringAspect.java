package com.miracle.smart_ecommerce_security.aspects;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

/**
 * Aspect for monitoring transaction execution - OPTIMIZED.
 * Only logs slow transactions and errors.
 */
@Aspect
@Component
@Slf4j
public class TransactionMonitoringAspect {

    private static final long SLOW_TRANSACTION_MS = 1000;
    private static final boolean DETAILED_LOGGING = false;

    @Around("@annotation(org.springframework.transaction.annotation.Transactional)")
    public Object monitorTransaction(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            
            // Only log slow transactions
            if (duration > SLOW_TRANSACTION_MS) {
                String className = joinPoint.getTarget().getClass().getSimpleName();
                String methodName = joinPoint.getSignature().getName();
                boolean wasRolledBack = TransactionAspectSupport.currentTransactionStatus().isRollbackOnly();
                
                if (wasRolledBack) {
                    log.warn("Transaction rolled back for {}.{} after {}ms", className, methodName, duration);
                } else {
                    log.warn("SLOW TRANSACTION: {}.{} took {}ms", className, methodName, duration);
                }
            }
            
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            String className = joinPoint.getTarget().getClass().getSimpleName();
            String methodName = joinPoint.getSignature().getName();
            log.error("Transaction failed for {}.{} after {}ms - Exception: {}", 
                className, methodName, duration, e.getMessage());
            throw e;
        }
    }
}
