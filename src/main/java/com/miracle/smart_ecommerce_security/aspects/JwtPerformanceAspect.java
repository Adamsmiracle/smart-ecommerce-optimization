package com.miracle.smart_ecommerce_security.aspects;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Lightweight performance monitor specifically for JWT token operations.
 * Uses minimal overhead logging to avoid interfering with token validation performance.
 */
@Aspect
@Component
public class JwtPerformanceAspect {

    private static final Logger log = LoggerFactory.getLogger(JwtPerformanceAspect.class);
    private static final long SLOW_JWT_THRESHOLD_MS = 100;

    /**
     * Monitor only the validateToken method for performance issues
     */
    @Pointcut("execution(* com.miracle.smart_ecommerce_security.domain.auth.service.impl.JwtTokenService.validateToken(..))")
    public void validateTokenMethod() {}

    @Around("validateTokenMethod()")
    public Object monitorJwtValidation(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.nanoTime();
        
        try {
            Object result = joinPoint.proceed();
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            
            // Only log if slow (>100ms) to avoid log spam
            if (durationMs >= SLOW_JWT_THRESHOLD_MS) {
                log.warn("JWT validation took {} ms (threshold: {}ms)", durationMs, SLOW_JWT_THRESHOLD_MS);
            }
            
            return result;
        } catch (Throwable throwable) {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            log.error("JWT validation failed after {} ms: {}", durationMs, throwable.getMessage());
            throw throwable;
        }
    }
}
