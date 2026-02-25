package com.miracle.smart_ecommerce_jpa.controller;

import com.miracle.smart_ecommerce_jpa.common.response.ApiResponse;
import com.miracle.smart_ecommerce_jpa.service.CacheManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for cache management and performance monitoring.
 * Provides endpoints to monitor, manage, and analyze cache performance.
 */
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
@Tag(name = "Cache Management", description = "Cache monitoring and management endpoints")
@Slf4j
public class CacheManagementController {

    private final CacheManagementService cacheManagementService;

    @GetMapping("/statistics")
    @Operation(summary = "Get cache statistics", description = "Retrieve detailed statistics for all caches including hit rates, sizes, and performance metrics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCacheStatistics() {
        log.debug("Retrieving cache statistics");
        Map<String, Object> stats = cacheManagementService.getCacheStatistics();
        return ResponseEntity.ok(ApiResponse.success(stats, "Cache statistics retrieved successfully"));
    }

    @GetMapping("/health")
    @Operation(summary = "Get cache health", description = "Check the health status of all caches with performance indicators")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCacheHealth() {
        log.debug("Checking cache health");
        Map<String, Object> health = cacheManagementService.getCacheHealth();
        return ResponseEntity.ok(ApiResponse.success(health, "Cache health check completed"));
    }

    @GetMapping("/recommendations")
    @Operation(summary = "Get performance recommendations", description = "Get performance recommendations based on cache statistics and usage patterns")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPerformanceRecommendations() {
        log.debug("Generating performance recommendations");
        Map<String, Object> recommendations = cacheManagementService.getPerformanceRecommendations();
        return ResponseEntity.ok(ApiResponse.success(recommendations, "Performance recommendations generated"));
    }

    @PostMapping("/warmup/products")
    @Operation(summary = "Warm up product cache", description = "Preload frequently accessed products into cache")
    public ResponseEntity<ApiResponse<String>> warmUpProductCache() {
        log.info("Starting product cache warm-up");
        CompletableFuture<Void> warmUp = cacheManagementService.warmUpProductCache();
        warmUp.thenRun(() -> log.info("Product cache warm-up completed"))
          .exceptionally(e -> { log.error("Product cache warm-up failed", e); return null; });
        
        return ResponseEntity.ok(ApiResponse.success("Product cache warm-up initiated", "Cache warm-up started successfully"));
    }

    @PostMapping("/warmup/users")
    @Operation(summary = "Warm up user cache", description = "Preload active user profiles into cache")
    public ResponseEntity<ApiResponse<String>> warmUpUserCache() {
        log.info("Starting user cache warm-up");
        CompletableFuture<Void> warmUp = cacheManagementService.warmUpUserCache();
        warmUp.thenRun(() -> log.info("User cache warm-up completed"))
          .exceptionally(e -> { log.error("User cache warm-up failed", e); return null; });
        
        return ResponseEntity.ok(ApiResponse.success("User cache warm-up initiated", "Cache warm-up started successfully"));
    }

    @PostMapping("/warmup/categories")
    @Operation(summary = "Warm up category cache", description = "Preload categories with product counts into cache")
    public ResponseEntity<ApiResponse<String>> warmUpCategoryCache() {
        log.info("Starting category cache warm-up");
        CompletableFuture<Void> warmUp = cacheManagementService.warmUpCategoryCache();
        warmUp.thenRun(() -> log.info("Category cache warm-up completed"))
          .exceptionally(e -> { log.error("Category cache warm-up failed", e); return null; });
        
        return ResponseEntity.ok(ApiResponse.success("Category cache warm-up initiated", "Cache warm-up started successfully"));
    }

    @DeleteMapping("/products")
    @Operation(summary = "Clear product cache", description = "Evict all entries from the product cache")
    public ResponseEntity<ApiResponse<String>> clearProductCache() {
        log.info("Clearing product cache");
        cacheManagementService.evictProductCache();
        return ResponseEntity.ok(ApiResponse.success("Product cache cleared", "Product cache cleared successfully"));
    }

    @DeleteMapping("/users")
    @Operation(summary = "Clear user cache", description = "Evict all entries from the user cache")
    public ResponseEntity<ApiResponse<String>> clearUserCache() {
        log.info("Clearing user cache");
        cacheManagementService.evictUserCache();
        return ResponseEntity.ok(ApiResponse.success("User cache cleared", "User cache cleared successfully"));
    }

    @DeleteMapping("/categories")
    @Operation(summary = "Clear category cache", description = "Evict all entries from the category cache")
    public ResponseEntity<ApiResponse<String>> clearCategoryCache() {
        log.info("Clearing category cache");
        cacheManagementService.evictCategoryCache();
        return ResponseEntity.ok(ApiResponse.success("Category cache cleared", "Category cache cleared successfully"));
    }

    @DeleteMapping("/orders")
    @Operation(summary = "Clear order cache", description = "Evict all entries from the order cache")
    public ResponseEntity<ApiResponse<String>> clearOrderCache() {
        log.info("Clearing order cache");
        cacheManagementService.evictOrderCache();
        return ResponseEntity.ok(ApiResponse.success("Order cache cleared", "Order cache cleared successfully"));
    }

    @DeleteMapping("/all")
    @Operation(summary = "Clear all caches", description = "Evict all entries from all caches in the application")
    public ResponseEntity<ApiResponse<String>> clearAllCaches() {
        log.info("Clearing all caches");
        cacheManagementService.clearAllCaches();
        return ResponseEntity.ok(ApiResponse.success("All caches cleared", "All caches cleared successfully"));
    }

    @GetMapping("/performance")
    @Operation(summary = "Get comprehensive performance report", description = "Get detailed performance report including cache statistics, health, and recommendations")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPerformanceReport() {
        log.debug("Generating comprehensive performance report");
        
        Map<String, Object> report = Map.of(
            "statistics", cacheManagementService.getCacheStatistics(),
            "health", cacheManagementService.getCacheHealth(),
            "recommendations", cacheManagementService.getPerformanceRecommendations(),
            "timestamp", java.time.Instant.now()
        );
        
        return ResponseEntity.ok(ApiResponse.success(report, "Performance report generated successfully"));
    }
}
