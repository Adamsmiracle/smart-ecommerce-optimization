package com.miracle.smart_ecommerce_jpa.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Cache Configuration using Caffeine for the application.
 *
 * Strategy: Single cache per entity type with different key prefixes
 * - Keys: "id:{uuid}", "email:{email}", "list:page:{page}:size:{size}"
 * - Each cache: 5000 entries max, 30-minute TTL
 * - Write operations update/evict only affected keys via CacheManager
 */
@Configuration
@EnableCaching
public class CacheConfig {

    // Cache names - one per entity type
    public static final String USERS_CACHE = "users";
    public static final String PRODUCTS_CACHE = "products";
    public static final String CATEGORIES_CACHE = "categories";
    public static final String ORDERS_CACHE = "orders";
    public static final String ADDRESSES_CACHE = "addresses";
    public static final String CART_CACHE = "cart";
    public static final String REVIEWS_CACHE = "reviews";
    public static final String PAYMENT_METHODS_CACHE = "payment_methods";
    public static final String SHIPPING_METHODS_CACHE = "shipping_method";

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cacheManager = new SimpleCacheManager();

        cacheManager.setCaches(Arrays.asList(
            buildEntityCache(USERS_CACHE),
            buildEntityCache(PRODUCTS_CACHE),
            buildEntityCache(CATEGORIES_CACHE),
            buildEntityCache(ORDERS_CACHE),
            buildEntityCache(ADDRESSES_CACHE),
            buildEntityCache(CART_CACHE),
            buildEntityCache(PAYMENT_METHODS_CACHE),
            buildEntityCache(REVIEWS_CACHE),
            buildEntityCache(SHIPPING_METHODS_CACHE)
        ));

        return cacheManager;
    }

    /**
     * Build cache for entity with unified storage
     * - Stores all query types (byId, byEmail, list) in same cache
     * - Uses key prefixes to distinguish: "id:{uuid}", "email:{email}", "list:..."
     * - Medium size, moderate TTL
     */
    private CaffeineCache buildEntityCache(String name) {
        return new CaffeineCache(name, Caffeine.newBuilder()
                .maximumSize(5000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats()
                .build());
    }
}

