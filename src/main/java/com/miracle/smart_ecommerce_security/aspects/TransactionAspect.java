package com.miracle.smart_ecommerce_security.aspects;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Aspect for monitoring transactional operations.
 * Logs transaction boundaries and rollbacks.
 */
@Aspect
@Component
@Slf4j
public class TransactionAspect {

    /**
     * Pointcut for methods annotated with @Transactional
     */
    @Pointcut("@annotation(org.springframework.transaction.annotation.Transactional)")
    public void transactionalMethods() {}

    /**
     * Monitor transactional method execution
     */
    @Around("transactionalMethods() && @annotation(transactional)")
    public Object monitorTransaction(ProceedingJoinPoint joinPoint, Transactional transactional) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        boolean readOnly = transactional.readOnly();
        long startTime = System.currentTimeMillis();

        log.debug("TRANSACTION START - Method: {} | ReadOnly: {}", methodName, readOnly);

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            log.debug("TRANSACTION COMMIT - Method: {} | Duration: {} ms", methodName, executionTime);

            if (executionTime > 1000) {
                log.warn("LONG TRANSACTION - Method {} took {} ms", methodName, executionTime);
            }

            return result;
        } catch (Throwable throwable) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("TRANSACTION ROLLBACK - Method: {} | Duration: {} ms | Reason: {}",
                     methodName, executionTime, throwable.getMessage());
            throw throwable;
        }
    }
}

