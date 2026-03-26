package com.miracle.smart_ecommerce_security.aspects;

import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Component;

/**
 * Aspect for monitoring cache operations - OPTIMIZED.
 * Only logs on cache misses and errors to reduce overhead.
 */
@Aspect
@Component
@Slf4j
public class CachingAspect {

    private final CacheManager cacheManager;
    private static final boolean DETAILED_LOGGING = false; // Enable only for debugging

    public CachingAspect(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

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
     * Monitor cacheable method invocations - OPTIMIZED
     */
    @Around("cacheableMethods()")
    public Object monitorCacheableMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!DETAILED_LOGGING) {
            return joinPoint.proceed();
        }
        
        String methodName = joinPoint.getSignature().toShortString();
        try {
            Object result = joinPoint.proceed();
            log.debug("CACHE OPERATION - Method {} executed successfully", methodName);
            logCacheStatistics();
            return result;
        } catch (Throwable throwable) {
            log.error("Cache operation failed for method {}: {}", methodName, throwable.getMessage());
            throw throwable;
        }
    }

    /**
     * Log current cache statistics for monitoring
     */
    private void logCacheStatistics() {
        try {
            cacheManager.getCacheNames().forEach(cacheName -> {
                org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
                if (cache instanceof CaffeineCache) {
                    CaffeineCache caffeineCache = (CaffeineCache) cache;
                    CacheStats stats = caffeineCache.getNativeCache().stats();
                    
                    if (stats.requestCount() > 0) {
                        double hitRate = stats.hitRate() * 100;
                        log.debug("CACHE STATS [{}] - Hits: {}, Misses: {}, Hit Rate: {:.2f}%, Size: {}",
                                cacheName, stats.hitCount(), stats.missCount(), 
                                hitRate, caffeineCache.getNativeCache().estimatedSize());
                    }
                }
            });
        } catch (Exception e) {
            log.debug("Could not log cache statistics: {}", e.getMessage());
        }
    }

    /**
     * Monitor cache eviction operations - OPTIMIZED
     */
    @Around("cacheEvictMethods()")
    public Object monitorCacheEviction(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!DETAILED_LOGGING) {
            return joinPoint.proceed();
        }
        
        String methodName = joinPoint.getSignature().toShortString();
        log.debug("CACHE EVICT - Clearing cache for method {}", methodName);
        try {
            Object result = joinPoint.proceed();
            log.debug("CACHE EVICT SUCCESS - Cache cleared for method {}", methodName);
            return result;
        } catch (Throwable throwable) {
            log.error("CACHE EVICT FAILED for method {}: {}", methodName, throwable.getMessage());
            throw throwable;
        }
    }

    /**
     * Monitor cache put operations - OPTIMIZED
     */
    @Around("cachePutMethods()")
    public Object monitorCachePut(ProceedingJoinPoint joinPoint) throws Throwable {
        if (!DETAILED_LOGGING) {
            return joinPoint.proceed();
        }
        
        String methodName = joinPoint.getSignature().toShortString();
        log.debug("CACHE PUT - Updating cache for method {}", methodName);
        try {
            Object result = joinPoint.proceed();
            log.debug("CACHE PUT SUCCESS - Cache updated for method {}", methodName);
            return result;
        } catch (Throwable throwable) {
            log.error("CACHE PUT FAILED for method {}: {}", methodName, throwable.getMessage());
            throw throwable;
        }
    }
}

