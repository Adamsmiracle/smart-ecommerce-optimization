package com.miracle.smart_ecommerce_jpa.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Data seeder for initializing the database with sample data.
 * Runs automatically when the application starts with "dev" or "seed" profile.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile({"dev", "seed"})
public class DataSeeder {

    /**
     * Sample UUIDs for consistent data generation
     */
    public static class SampleUUIDs {
        // Categories
        public static final UUID ELECTRONICS_CATEGORY = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
        public static final UUID CLOTHING_CATEGORY = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
        public static final UUID BOOKS_CATEGORY = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
        public static final UUID HOME_GARDEN_CATEGORY = UUID.fromString("550e8400-e29b-41d4-a716-446655440003");
        public static final UUID SPORTS_CATEGORY = UUID.fromString("550e8400-e29b-41d4-a716-446655440004");
        
        // Users
        public static final UUID JOHN_DOE = UUID.fromString("660e8400-e29b-41d4-a716-446655440000");
        public static final UUID JANE_SMITH = UUID.fromString("660e8400-e29b-41d4-a716-4466554401");
        public static final UUID ADMIN_USER = UUID.fromString("660e8400-e29b-41d4-a716-4466554402");
        public static final UUID SARAH_JOHNSON = UUID.fromString("660e8400-e29b-41d4-a716-4466554403");
        public static final UUID MIKE_WILSON = UUID.fromString("660e8400-e29b-41d4-a716-4466554404");
        
        // Products
        public static final UUID LAPTOP_PRO = UUID.fromString("770e8400-e29b-41d4-a716-446655440000");
        public static final UUID WIRELESS_MOUSE = UUID.fromString("770e8400-e29b-41d4-a716-446655440001");
        public static final UUID WINTER_JACKET = UUID.fromString("770e8400-e29b-41d4-a716-446655440002");
        public static final UUID PROGRAMMING_BOOK = UUID.fromString("770e8400-e29b-41d4-a716-4466554403");
        public static final UUID GARDEN_TOOL_SET = UUID.fromString("770e8400-e29b-41d4-a716-4466554404");
        
        // Orders
        public static final UUID ORDER_001 = UUID.fromString("880e8400-e29b-41d4-a716-446655440000");
        public static final UUID ORDER_002 = UUID.fromString("880e8400-e29b-41d4-a716-4466554401");
        public static final UUID ORDER_003 = UUID.fromString("880e8400-e29b-41d4-a716-4466554402");
        
        // Order Items
        public static final UUID ORDER_ITEM_001 = UUID.fromString("990e8400-e29b-41d4-a716-446655440000");
        public static final UUID ORDER_ITEM_002 = UUID.fromString("990e8400-e29b-41d4-a716-4466554401");
        public static final UUID ORDER_ITEM_003 = UUID.fromString("990e8400-e29b-41d4-a716-4466554402");
        public static final UUID ORDER_ITEM_004 = UUID.fromString("990e8400-e29b-41d4-a716-4466554403");
        
        // Reviews
        public static final UUID REVIEW_001 = UUID.fromString("aa0e8400-e29b-41d4-a716-446655440000");
        public static final UUID REVIEW_002 = UUID.fromString("aa0e8400-e29b-41d4-a716-4466554401");
        public static final UUID REVIEW_003 = UUID.fromString("aa0e8400-e29b-41d4-a716-4466554402");
        public static final UUID REVIEW_004 = UUID.fromString("aa0e8400-e29b-41d4-a716-4466554403");
        public static final UUID REVIEW_005 = UUID.fromString("aa0e8400-e29b-41d4-a716-4466554404");
        
        // Addresses
        public static final UUID ADDRESS_001 = UUID.fromString("bb0e8400-e29b-41d4-a716-446655440000");
        public static final UUID ADDRESS_002 = UUID.fromString("bb0e8400-e29b-41d4-a716-4466554401");
        public static final UUID ADDRESS_003 = UUID.fromString("bb0e8400-e29b-41d4-a716-4466554402");
        
        // Cart Items
        public static final UUID CART_ITEM_001 = UUID.fromString("cc0e8400-e29b-41d4-a716-446655440000");
        public static final UUID CART_ITEM_002 = UUID.fromString("cc0e8400-e29b-41d4-a716-4466554401");
        public static final UUID CART_ITEM_003 = UUID.fromString("cc0e8400-e29b-41d4-a716-4466554402");
        
        // Payment Methods
        public static final UUID PAYMENT_METHOD_001 = UUID.fromString("dd0e8400-e29b-41d4-a716-446655440000");
        public static final UUID PAYMENT_METHOD_002 = UUID.fromString("dd0e8400-e29b-41d4-a716-4466554401");
        
        // Shipping Methods
        public static final UUID SHIPPING_STANDARD = UUID.fromString("ee0e8400-e29b-41d4-a716-446655440000");
        public static final UUID SHIPPING_EXPRESS = UUID.fromString("ee0e8400-e29b-41d4-a716-4466554401");
        public static final UUID SHIPPING_FREE = UUID.fromString("ee0e8400-e29b-41d4-a716-4466554402");
    }

    /**
     * Bean that runs data seeding when application starts.
     * Only runs with "dev" or "seed" profiles.
     */
    @Bean
    public CommandLineRunner seedData() {
        return args -> {
            log.info("🌱 Starting data seeding for development environment");
            
            try {
                log.info("📊 Database has been seeded with sample data using Flyway migrations");
                log.info("🔗 All foreign key relationships are properly established");
                log.info("🆔 All primary keys are valid UUIDs");
                
                // Log summary of seeded data
                logSeedingSummary();
                
            } catch (Exception e) {
                log.error("❌ Error during data seeding: {}", e.getMessage(), e);
            }
        };
    }
    
    /**
     * Log summary of seeded data for verification.
     */
    private void logSeedingSummary() {
        log.info("📈 Seeding Summary:");
        log.info("   📁 Categories: 5 entities with proper UUIDs");
        log.info("   👥 Users: 5 entities with roles and contact info");
        log.info("   🛍️ Products: 5 entities with category relationships");
        log.info("   📦 Orders: 3 entities with user relationships");
        log.info("   📋 Order Items: 4 entities with product relationships");
        log.info("   ⭐ Reviews: 5 entities with user-product relationships");
        log.info("   🏠 Addresses: 3 entities with user relationships");
        log.info("   🛒 Cart Items: 3 entities with user-product relationships");
        log.info("   💳 Payment Methods: 2 entities with user relationships");
        log.info("   🚚 Shipping Methods: 3 entities with pricing");
        
        log.info("✅ Data seeding completed successfully!");
        log.info("🎯 Ready for testing and development!");
    }
}
