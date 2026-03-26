package com.miracle.smart_ecommerce_security.domain.auth.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for monitoring token validation performance and cache effectiveness.
 * Provides metrics on cache hit rates, validation times, and overall performance.
 */
@Service
@RequiredArgsConstructor
public class TokenPerformanceService {

    private final CacheManager cacheManager;

    /**
     * Get comprehensive token cache performance metrics.
     * 
     * @return Map containing cache statistics and performance indicators
     */
    public Map<String, Object> getTokenCacheMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        org.springframework.cache.Cache springCache = cacheManager.getCache("token");
        if (springCache instanceof CaffeineCache caffeineCache) {
            Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
            CacheStats stats = nativeCache.stats();
            
            long hitCount = stats.hitCount();
            long missCount = stats.missCount();
            long totalRequests = hitCount + missCount;
            double hitRate = totalRequests > 0 ? (hitCount * 100.0 / totalRequests) : 0.0;
            
            metrics.put("hitCount", hitCount);
            metrics.put("missCount", missCount);
            metrics.put("totalRequests", totalRequests);
            metrics.put("hitRate", String.format("%.2f%%", hitRate));
            metrics.put("evictionCount", stats.evictionCount());
            metrics.put("loadSuccessCount", stats.loadSuccessCount());
            metrics.put("loadFailureCount", stats.loadFailureCount());
            metrics.put("averageLoadPenalty", String.format("%.2f ms", stats.averageLoadPenalty() / 1_000_000.0));
            metrics.put("estimatedSize", nativeCache.estimatedSize());
            
            // Performance assessment
            String performance;
            if (hitRate >= 80) {
                performance = "EXCELLENT";
            } else if (hitRate >= 60) {
                performance = "GOOD";
            } else if (hitRate >= 40) {
                performance = "FAIR";
            } else {
                performance = "POOR";
            }
            metrics.put("performance", performance);
            
            // Recommendations
            if (hitRate < 60) {
                metrics.put("recommendation", "Consider increasing cache TTL or capacity");
            } else if (stats.evictionCount() > totalRequests * 0.1) {
                metrics.put("recommendation", "High eviction rate - consider increasing cache size");
            } else {
                metrics.put("recommendation", "Cache is performing optimally");
            }
        } else {
            metrics.put("error", "Token cache not found or not a Caffeine cache");
        }
        
        return metrics;
    }

    /**
     * Clear the token cache (useful for testing or emergency scenarios).
     */
    public void clearTokenCache() {
        org.springframework.cache.Cache cache = cacheManager.getCache("token");
        if (cache != null) {
            cache.clear();
        }
    }

    /**
     * Get cache size and capacity information.
     */
    public Map<String, Object> getCacheCapacityInfo() {
        Map<String, Object> info = new HashMap<>();
        
        org.springframework.cache.Cache springCache = cacheManager.getCache("token");
        if (springCache instanceof CaffeineCache caffeineCache) {
            Cache<Object, Object> nativeCache = caffeineCache.getNativeCache();
            long estimatedSize = nativeCache.estimatedSize();
            long maxSize = 10000; // From CacheConfig
            
            info.put("currentSize", estimatedSize);
            info.put("maxSize", maxSize);
            info.put("utilizationPercent", String.format("%.2f%%", (estimatedSize * 100.0 / maxSize)));
            info.put("availableCapacity", maxSize - estimatedSize);
        }
        
        return info;
    }
}
