package com.miracle.smart_ecommerce_jpa.service;

import com.miracle.smart_ecommerce_jpa.config.CacheConfig;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service for cache management and performance monitoring.
 * Provides cache operations, statistics, and optimization strategies.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CacheManagementService {

    private final CacheManager cacheManager;

    /**
     * Get cache statistics for all caches.
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // Get cache names and basic statistics
            Map<String, Object> productsStats = getBasicCacheStats(CacheConfig.PRODUCTS_CACHE);
            Map<String, Object> usersStats = getBasicCacheStats(CacheConfig.USERS_CACHE);
            Map<String, Object> categoriesStats = getBasicCacheStats(CacheConfig.CATEGORIES_CACHE);
            Map<String, Object> ordersStats = getBasicCacheStats(CacheConfig.ORDERS_CACHE);
            
            stats.put("products", productsStats);
            stats.put("users", usersStats);
            stats.put("categories", categoriesStats);
            stats.put("orders", ordersStats);
            
            // Overall statistics
            long totalHits = getTotalHits();
            long totalMisses = getTotalMisses();
            long totalRequests = totalHits + totalMisses;
            double overallHitRate = totalRequests > 0 ? (double) totalHits / totalRequests * 100 : 0.0;
            
            stats.put("overall", Map.of(
                "totalHits", totalHits,
                "totalMisses", totalMisses,
                "totalRequests", totalRequests,
                "overallHitRate", overallHitRate,
                "timestamp", Instant.now()
            ));
            
        } catch (Exception e) {
            log.error("Error getting cache statistics: {}", e.getMessage(), e);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * Get basic statistics for a specific cache.
     */
    private Map<String, Object> getBasicCacheStats(String cacheName) {
        try {
            org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
            if (cache instanceof CaffeineCache) {
                CaffeineCache caffeineCache = (CaffeineCache) cache;
                CacheStats stats = caffeineCache.getNativeCache().stats();
                
                return Map.of(
                    "status", "active",
                    "cacheName", cacheName,
                    "message", "Cache is active and operational",
                    "hitCount", stats.hitCount(),
                    "missCount", stats.missCount(),
                    "hitRate", stats.hitRate(),
                    "size", caffeineCache.getNativeCache().estimatedSize(),
                    "evictionCount", stats.evictionCount(),
                    "loadSuccessCount", stats.loadSuccessCount()
                );
            } else if (cache != null) {
                return Map.of(
                    "status", "active",
                    "cacheName", cacheName,
                    "message", "Cache is active but stats not available"
                );
            } else {
                return Map.of(
                    "status", "inactive",
                    "cacheName", cacheName,
                    "message", "Cache not found or not initialized"
                );
            }
        } catch (Exception e) {
            return Map.of(
                "status", "error",
                "cacheName", cacheName,
                "message", e.getMessage()
            );
        }
    }
    
    /**
     * Get total hits across all caches.
     */
    private long getTotalHits() {
        return cacheManager.getCacheNames().stream()
            .mapToLong(cacheName -> {
                org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
                if (cache instanceof CaffeineCache) {
                    return ((CaffeineCache) cache).getNativeCache().stats().hitCount();
                }
                return 0L;
            })
            .sum();
    }
    
    /**
     * Get total misses across all caches.
     */
    private long getTotalMisses() {
        return cacheManager.getCacheNames().stream()
            .mapToLong(cacheName -> {
                org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
                if (cache instanceof CaffeineCache) {
                    return ((CaffeineCache) cache).getNativeCache().stats().missCount();
                }
                return 0L;
            })
            .sum();
    }

    /**
     * Warm up caches with frequently accessed data.
     */
    @Cacheable(value = CacheConfig.PRODUCTS_CACHE, key = "'warmup:active-products'")
    public CompletableFuture<Void> warmUpProductCache() {
        return CompletableFuture.runAsync(() -> {
            log.info("Starting product cache warm-up");
            // This would typically be called by a scheduled job
            // Implementation would load frequently accessed products
            log.info("Product cache warm-up completed");
        });
    }

    /**
     * Warm up user cache with active users.
     */
    @Cacheable(value = CacheConfig.USERS_CACHE, key = "'warmup:active-users'")
    public CompletableFuture<Void> warmUpUserCache() {
        return CompletableFuture.runAsync(() -> {
            log.info("Starting user cache warm-up");
            // Load active users into cache
            log.info("User cache warm-up completed");
        });
    }

    /**
     * Warm up category cache.
     */
    @Cacheable(value = CacheConfig.CATEGORIES_CACHE, key = "'warmup:categories'")
    public CompletableFuture<Void> warmUpCategoryCache() {
        return CompletableFuture.runAsync(() -> {
            log.info("Starting category cache warm-up");
            // Load categories with product counts
            log.info("Category cache warm-up completed");
        });
    }

    /**
     * Clear specific cache by name.
     */
    public void clearCache(String cacheName) {
        try {
            org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
                log.info("Cleared cache: {}", cacheName);
            }
        } catch (Exception e) {
            log.error("Error clearing cache {}: {}", cacheName, e.getMessage(), e);
        }
    }

    /**
     * Clear all caches.
     */
    public void clearAllCaches() {
        try {
            cacheManager.getCacheNames().forEach(cacheName -> {
                org.springframework.cache.Cache cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            });
            log.info("Cleared all caches");
        } catch (Exception e) {
            log.error("Error clearing all caches: {}", e.getMessage(), e);
        }
    }

    /**
     * Evict specific entries from product cache.
     */
    @CacheEvict(value = CacheConfig.PRODUCTS_CACHE, allEntries = true)
    public void evictProductCache() {
        log.info("Evicted all entries from product cache");
    }

    /**
     * Evict specific entries from user cache.
     */
    @CacheEvict(value = CacheConfig.USERS_CACHE, allEntries = true)
    public void evictUserCache() {
        log.info("Evicted all entries from user cache");
    }

    /**
     * Evict specific entries from category cache.
     */
    @CacheEvict(value = CacheConfig.CATEGORIES_CACHE, allEntries = true)
    public void evictCategoryCache() {
        log.info("Evicted all entries from category cache");
    }

    /**
     * Evict specific entries from order cache.
     */
    @CacheEvict(value = CacheConfig.ORDERS_CACHE, allEntries = true)
    public void evictOrderCache() {
        log.info("Evicted all entries from order cache");
    }

    /**
     * Get performance recommendations based on cache statistics.
     */
    public Map<String, Object> getPerformanceRecommendations() {
        Map<String, Object> recommendations = new HashMap<>();
        Map<String, Object> stats = getCacheStatistics();
        
        try {
            // Analyze hit rates
            Map<String, Object> productStats = (Map<String, Object>) stats.get("products");
            if (productStats != null) {
                double hitRate = ((Number) productStats.get("hitRate")).doubleValue();
                if (hitRate < 70) {
                    recommendations.put("products", Map.of(
                        "status", "warning",
                        "message", "Product cache hit rate is low (<70%). Consider increasing cache size or TTL.",
                        "currentHitRate", hitRate,
                        "recommendation", "Increase cache size to 10000 or TTL to 60 minutes"
                    ));
                } else if (hitRate > 95) {
                    recommendations.put("products", Map.of(
                        "status", "good",
                        "message", "Product cache hit rate is excellent (>95%).",
                        "currentHitRate", hitRate
                    ));
                }
            }
            
            Map<String, Object> userStats = (Map<String, Object>) stats.get("users");
            if (userStats != null) {
                double hitRate = ((Number) userStats.get("hitRate")).doubleValue();
                if (hitRate < 80) {
                    recommendations.put("users", Map.of(
                        "status", "warning", 
                        "message", "User cache hit rate could be improved. Consider cache warm-up strategies.",
                        "currentHitRate", hitRate
                    ));
                }
            }
            
            // Memory usage recommendations
            stats.forEach((cacheName, cacheStats) -> {
                if (cacheStats instanceof Map) {
                    Map<String, Object> statMap = (Map<String, Object>) cacheStats;
                    Number size = (Number) statMap.get("size");
                    if (size != null && size.longValue() > 4000) {
                        recommendations.put(cacheName + "_memory", Map.of(
                            "status", "warning",
                            "message", "Cache size is approaching limit. Consider monitoring memory usage.",
                            "currentSize", size.longValue(),
                            "recommendation", "Monitor JVM memory and consider cache partitioning"
                        ));
                    }
                }
            });
            
        } catch (Exception e) {
            log.error("Error generating performance recommendations: {}", e.getMessage(), e);
            recommendations.put("error", e.getMessage());
        }
        
        return recommendations;
    }

    /**
     * Get cache health status.
     */
    public Map<String, Object> getCacheHealth() {
        Map<String, Object> health = new HashMap<>();
        Map<String, Object> stats = getCacheStatistics();
        
        try {
            stats.forEach((cacheName, cacheStats) -> {
                if (cacheStats instanceof Map) {
                    Map<String, Object> statMap = (Map<String, Object>) cacheStats;
                    double hitRate = ((Number) statMap.get("hitRate")).doubleValue();
                    long size = ((Number) statMap.get("size")).longValue();
                    
                    String status;
                    if (hitRate >= 90 && size <= 3000) {
                        status = "healthy";
                    } else if (hitRate >= 70 && size <= 4000) {
                        status = "warning";
                    } else {
                        status = "critical";
                    }
                    
                    health.put(cacheName, Map.of(
                        "status", status,
                        "hitRate", hitRate,
                        "size", size,
                        "lastChecked", Instant.now()
                    ));
                }
            });
            
        } catch (Exception e) {
            log.error("Error getting cache health: {}", e.getMessage(), e);
            health.put("error", e.getMessage());
        }
        
        return health;
    }
}
