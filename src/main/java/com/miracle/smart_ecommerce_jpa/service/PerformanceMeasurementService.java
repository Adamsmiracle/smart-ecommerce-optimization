package com.miracle.smart_ecommerce_jpa.service;

import com.miracle.smart_ecommerce_jpa.config.CacheConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Service for measuring and reporting cache and application performance.
 * Provides detailed metrics, timing, and performance analysis.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PerformanceMeasurementService {

    private final CacheManager cacheManager;
    private final MeterRegistry meterRegistry;
    
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong cacheHits = new AtomicLong(0);
    private final AtomicLong cacheMisses = new AtomicLong(0);

    /**
     * Record a cache hit.
     */
    public void recordCacheHit(String cacheName) {
        cacheHits.incrementAndGet();
        meterRegistry.counter("cache.hits", "cache", cacheName).increment();
    }

    /**
     * Record a cache miss.
     */
    public void recordCacheMiss(String cacheName) {
        cacheMisses.incrementAndGet();
        meterRegistry.counter("cache.misses", "cache", cacheName).increment();
    }

    /**
     * Record execution time of an operation.
     */
    public void recordExecutionTime(String operation, long durationMs) {
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("operation.execution.time")
                .tag("operation", operation)
                .register(meterRegistry));
    }

    /**
     * Get comprehensive performance metrics.
     */
    public Map<String, Object> getPerformanceMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            // Cache performance metrics
            metrics.put("cache", getCacheMetrics());
            
            // System performance metrics
            metrics.put("system", getSystemMetrics());
            
            // Request statistics
            metrics.put("requests", Map.of(
                "total", totalRequests.get(),
                "cacheHits", cacheHits.get(),
                "cacheMisses", cacheMisses.get(),
                "hitRate", calculateHitRate()
            ));
            
            // Memory usage
            metrics.put("memory", getMemoryMetrics());
            
            // Response time metrics
            metrics.put("responseTimes", getResponseTimeMetrics());
            
        } catch (Exception e) {
            log.error("Error collecting performance metrics: {}", e.getMessage(), e);
            metrics.put("error", e.getMessage());
        }
        
        return metrics;
    }

    /**
     * Get cache-specific metrics.
     */
    private Map<String, Object> getCacheMetrics() {
        Map<String, Object> cacheMetrics = new HashMap<>();
        
        try {
            // Get individual cache statistics
            List<String> cacheNames = List.of(
                CacheConfig.PRODUCTS_CACHE, CacheConfig.USERS_CACHE, 
                CacheConfig.CATEGORIES_CACHE, CacheConfig.ORDERS_CACHE
            );
            
            for (String cacheName : cacheNames) {
                try {
                    var cache = cacheManager.getCache(cacheName);
                    if (cache != null) {
                        cacheMetrics.put(cacheName, Map.of(
                            "status", "active",
                            "message", "Cache is active and collecting statistics"
                        ));
                    } else {
                        cacheMetrics.put(cacheName, Map.of(
                            "status", "inactive",
                            "message", "Cache not found or not initialized"
                        ));
                    }
                } catch (Exception e) {
                    cacheMetrics.put(cacheName, Map.of(
                        "status", "error",
                        "message", e.getMessage()
                    ));
                }
            }
            
        } catch (Exception e) {
            log.error("Error getting cache metrics: {}", e.getMessage(), e);
            cacheMetrics.put("error", e.getMessage());
        }
        
        return cacheMetrics;
    }

    /**
     * Get system-level metrics.
     */
    private Map<String, Object> getSystemMetrics() {
        Map<String, Object> systemMetrics = new HashMap<>();
        
        try {
            Runtime runtime = Runtime.getRuntime();
            
            // Memory information
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            systemMetrics.put("memory", Map.of(
                "total", totalMemory,
                "used", usedMemory,
                "free", freeMemory,
                "max", maxMemory,
                "usagePercentage", (double) usedMemory / maxMemory * 100
            ));
            
            // Thread information
            systemMetrics.put("threads", Map.of(
                "active", Thread.activeCount(),
                "availableProcessors", runtime.availableProcessors()
            ));
            
        } catch (Exception e) {
            log.error("Error getting system metrics: {}", e.getMessage(), e);
            systemMetrics.put("error", e.getMessage());
        }
        
        return systemMetrics;
    }

    /**
     * Get response time metrics from Micrometer.
     */
    private Map<String, Object> getResponseTimeMetrics() {
        Map<String, Object> responseMetrics = new HashMap<>();
        
        try {
            // Check if meter registry has any timers
            if (meterRegistry.getMeters().isEmpty()) {
                responseMetrics.put("status", "no_data");
                responseMetrics.put("message", "No response time metrics available");
            } else {
                // Get available timers
                var timers = meterRegistry.getMeters().stream()
                    .filter(meter -> meter.getId().getName().equals("http.server.requests"))
                    .map(meter -> (Timer) meter)
                    .collect(Collectors.toList());
                
                if (timers.isEmpty()) {
                    responseMetrics.put("status", "no_data");
                    responseMetrics.put("message", "No HTTP request timers found");
                } else {
                    double avgResponseTime = timers.stream()
                        .mapToDouble(timer -> timer.mean(TimeUnit.of(ChronoUnit.MILLIS)))
                        .average()
                        .orElse(0.0);
                    
                    double maxResponseTime = timers.stream()
                        .mapToDouble(timer -> timer.max(TimeUnit.of(ChronoUnit.MILLIS)))
                        .max()
                        .orElse(0.0);
                    
                    long totalRequests = timers.stream()
                        .mapToLong(timer -> timer.count())
                        .sum();
                    
                    responseMetrics.put("status", "available");
                    responseMetrics.put("average", avgResponseTime);
                    responseMetrics.put("max", maxResponseTime);
                    responseMetrics.put("totalRequests", totalRequests);
                    responseMetrics.put("timestamp", Instant.now());
                }
            }
            
        } catch (Exception e) {
            log.error("Error getting response time metrics: {}", e.getMessage(), e);
            responseMetrics.put("error", e.getMessage());
        }
        
        return responseMetrics;
    }

    /**
     * Get memory usage metrics.
     */
    private Map<String, Object> getMemoryMetrics() {
        Map<String, Object> memoryMetrics = new HashMap<>();
        
        try {
            Runtime runtime = Runtime.getRuntime();
            
            // Memory information
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            long maxMemory = runtime.maxMemory();
            
            // Memory efficiency analysis
            double usagePercentage = (double) usedMemory / maxMemory * 100;
            String memoryStatus;
            if (usagePercentage > 90) {
                memoryStatus = "critical";
            } else if (usagePercentage > 75) {
                memoryStatus = "warning";
            } else {
                memoryStatus = "healthy";
            }
            
            memoryMetrics.put("total", totalMemory);
            memoryMetrics.put("used", usedMemory);
            memoryMetrics.put("free", freeMemory);
            memoryMetrics.put("max", maxMemory);
            memoryMetrics.put("usagePercentage", usagePercentage);
            memoryMetrics.put("status", memoryStatus);
            memoryMetrics.put("timestamp", Instant.now());
            
            // Memory recommendations
            if (usagePercentage > 85) {
                memoryMetrics.put("recommendation", 
                    "Memory usage is critical. Consider increasing heap size or optimizing cache sizes.");
            } else if (usagePercentage > 70) {
                memoryMetrics.put("recommendation", 
                    "Memory usage is high. Monitor cache hit rates and consider cache optimization.");
            } else {
                memoryMetrics.put("recommendation", 
                    "Memory usage is within acceptable range.");
            }
            
        } catch (Exception e) {
            log.error("Error getting memory metrics: {}", e.getMessage(), e);
            memoryMetrics.put("error", e.getMessage());
        }
        
        return memoryMetrics;
    }

    /**
     * Calculate cache hit rate.
     */
    private double calculateHitRate() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = hits + misses;
        return total > 0 ? (double) hits / total * 100 : 0.0;
    }

    /**
     * Reset performance counters.
     */
    public void resetCounters() {
        totalRequests.set(0);
        cacheHits.set(0);
        cacheMisses.set(0);
        log.info("Performance counters reset");
    }

    /**
     * Get performance summary for dashboard.
     */
    public Map<String, Object> getPerformanceSummary() {
        Map<String, Object> summary = new HashMap<>();
        
        try {
            double hitRate = calculateHitRate();
            Map<String, Object> memory = getMemoryMetrics();
            
            String overallStatus;
            if (hitRate >= 80 && ((Number) memory.get("usagePercentage")).doubleValue() < 75) {
                overallStatus = "excellent";
            } else if (hitRate >= 60 && ((Number) memory.get("usagePercentage")).doubleValue() < 85) {
                overallStatus = "good";
            } else if (hitRate >= 40 && ((Number) memory.get("usagePercentage")).doubleValue() < 90) {
                overallStatus = "fair";
            } else {
                overallStatus = "poor";
            }
            
            summary.put("status", overallStatus);
            summary.put("cacheHitRate", hitRate);
            summary.put("memoryUsage", memory.get("usagePercentage"));
            summary.put("totalRequests", totalRequests.get());
            summary.put("timestamp", Instant.now());
            
            // Performance score (0-100)
            int performanceScore = calculatePerformanceScore(hitRate, ((Number) memory.get("usagePercentage")).doubleValue());
            summary.put("performanceScore", performanceScore);
            
        } catch (Exception e) {
            log.error("Error generating performance summary: {}", e.getMessage(), e);
            summary.put("error", e.getMessage());
        }
        
        return summary;
    }

    /**
     * Calculate overall performance score.
     */
    private int calculatePerformanceScore(double hitRate, double memoryUsage) {
        int cacheScore = hitRate >= 80 ? 40 : hitRate >= 60 ? 25 : hitRate >= 40 ? 15 : 5;
        int memoryScore = memoryUsage <= 75 ? 40 : memoryUsage <= 85 ? 25 : memoryUsage <= 90 ? 15 : 5;
        
        return Math.min(100, cacheScore + memoryScore);
    }
}
