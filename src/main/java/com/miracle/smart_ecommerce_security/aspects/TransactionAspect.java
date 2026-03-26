package com.miracle.smart_ecommerce_security.aspects;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aspect for monitoring transactional operations - OPTIMIZED.
 * Only logs slow transactions and errors to reduce overhead.
 */
@Aspect
@Component
@Slf4j
public class TransactionAspect {

    private static final long SLOW_TRANSACTION_MS = 1000;
    private static final boolean DETAILED_LOGGING = false;

    /**
     * Pointcut for methods annotated with @Transactional
     */
    @Pointcut("@annotation(org.springframework.transaction.annotation.Transactional)")
    public void transactionalMethods() {}

    /**
     * Monitor transactional method execution - OPTIMIZED
     */
    @Around("transactionalMethods() && @annotation(transactional)")
    public Object monitorTransaction(ProceedingJoinPoint joinPoint, Transactional transactional) throws Throwable {
        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            // Only log slow transactions
            if (executionTime > SLOW_TRANSACTION_MS) {
                String methodName = joinPoint.getSignature().toShortString();
                log.warn("LONG TRANSACTION - Method {} took {} ms", methodName, executionTime);
            } else if (DETAILED_LOGGING) {
                String methodName = joinPoint.getSignature().toShortString();
                log.debug("TRANSACTION COMMIT - Method: {} | Duration: {} ms", methodName, executionTime);
            }

            return result;
        } catch (Throwable throwable) {
            long executionTime = System.currentTimeMillis() - startTime;
            String methodName = joinPoint.getSignature().toShortString();
            log.error("TRANSACTION ROLLBACK - Method: {} | Duration: {} ms | Reason: {}",
                     methodName, executionTime, throwable.getMessage());
            throw throwable;
        }
    }
}

