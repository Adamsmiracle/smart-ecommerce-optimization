package com.miracle.smart_ecommerce_jpa.aspects;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * Aspect for monitoring cache operations.
 * Logs cache hits, misses, and evictions.
 */
@Aspect
@Component
@Slf4j
public class CachingAspect {

    /**
     * Pointcut for methods annotated with @Cacheable
     */
    @Pointcut("@annotation(org.springframework.cache.annotation.Cacheable)")
    public void cacheableMethods() {}

    /**
     * Pointcut for methods annotated with @CacheEvict
     */
    @Pointcut("@annotation(org.springframework.cache.annotation.CacheEvict)")
    public void cacheEvictMethods() {}

    /**
     * Pointcut for methods annotated with @CachePut
     */
    @Pointcut("@annotation(org.springframework.cache.annotation.CachePut)")
    public void cachePutMethods() {}


    /**
     * Monitor cacheable method invocations
     */
    @Around("cacheableMethods()")
    public Object monitorCacheableMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        long startTime = System.currentTimeMillis();

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            if (executionTime < 10) {
                log.debug("CACHE HIT - Method {} returned in {} ms (likely from cache)",
                         methodName, executionTime);
            } else {
                log.debug("CACHE MISS - Method {} executed in {} ms (database query)",
                         methodName, executionTime);
            }

            return result;
        } catch (Throwable throwable) {
            log.error("Cache operation failed for method {}: {}",
                     methodName, throwable.getMessage());
            throw throwable;
        }
    }

    /**
     * Monitor cache eviction operations
     */
    @Around("cacheEvictMethods()")
    public Object monitorCacheEviction(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();

        log.debug("CACHE EVICT - Clearing cache for method {}", methodName);

        try {
            Object result = joinPoint.proceed();
            log.debug("CACHE EVICT SUCCESS - Cache cleared for method {}", methodName);
            return result;
        } catch (Throwable throwable) {
            log.error("CACHE EVICT FAILED for method {}: {}",
                     methodName, throwable.getMessage());
            throw throwable;
        }
    }

    /**
     * Monitor cache put operations
     */
    @Around("cachePutMethods()")
    public Object monitorCachePut(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();

        log.debug("CACHE PUT - Updating cache for method {}", methodName);

        try {
            Object result = joinPoint.proceed();
            log.debug("CACHE PUT SUCCESS - Cache updated for method {}", methodName);
            return result;
        } catch (Throwable throwable) {
            log.error("CACHE PUT FAILED for method {}: {}",
                     methodName, throwable.getMessage());
            throw throwable;
        }
    }
}

