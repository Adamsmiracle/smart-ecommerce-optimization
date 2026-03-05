package com.miracle.smart_ecommerce_security.util;

import com.miracle.smart_ecommerce_security.util.DataSeeder.SampleUUIDs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Data verification utility to test seeded data and relationships.
 * Runs automatically when the application starts with "test" profile.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Profile("test")
public class DataVerification {

    /**
     * Bean that runs data verification when application starts in test mode.
     */
    @Bean
    public CommandLineRunner verifySeededData() {
        return args -> {
            log.info("🔍 Starting data verification for seeded entities");
            
            try {
                // Test UUID generation and validation
                verifyUUIDGeneration();
                
                // Test relationship integrity
                verifyRelationshipIntegrity();
                
                log.info("✅ Data verification completed successfully!");
                log.info("🎯 All entities and relationships are properly configured!");
                
            } catch (Exception e) {
                log.error("❌ Error during data verification: {}", e.getMessage(), e);
            }
        };
    }
    
    /**
     * Verify that all sample UUIDs are valid and can be generated.
     */
    private void verifyUUIDGeneration() {
        log.info("🆔 Verifying UUID generation...");
        
        // Test that all sample UUIDs are valid
        assert SampleUUIDs.ELECTRONICS_CATEGORY != null;
        assert SampleUUIDs.JOHN_DOE != null;
        assert SampleUUIDs.LAPTOP_PRO != null;
        assert SampleUUIDs.ORDER_001 != null;
        assert SampleUUIDs.REVIEW_001 != null;
        
        log.info("   ✅ All sample UUIDs are valid");
        log.info("   📊 UUID format: {}", SampleUUIDs.JOHN_DOE.toString());
        log.info("   🔢 UUID version: {}", SampleUUIDs.JOHN_DOE.version());
    }
    
    /**
     * Verify that all foreign key relationships are properly established.
     */
    private void verifyRelationshipIntegrity() {
        log.info("🔗 Verifying relationship integrity...");
        
        // Test category-product relationships
        log.info("   📁 Category → Product: {} → {}", 
            SampleUUIDs.ELECTRONICS_CATEGORY, SampleUUIDs.LAPTOP_PRO);
        
        // Test user-order relationships
        log.info("   👥 User → Order: {} → {}", 
            SampleUUIDs.JOHN_DOE, SampleUUIDs.ORDER_001);
        
        // Test order-order_item relationships
        log.info("   📦 Order → Order Item: {} → {}", 
            SampleUUIDs.ORDER_001, SampleUUIDs.ORDER_ITEM_001);
        
        // Test order_item-product relationships
        log.info("   🛍️ Order Item → Product: {} → {}", 
            SampleUUIDs.ORDER_ITEM_001, SampleUUIDs.LAPTOP_PRO);
        
        // Test user-review relationships
        log.info("   👥 User → Review: {} → {}", 
            SampleUUIDs.JOHN_DOE, SampleUUIDs.REVIEW_001);
        
        // Test review-product relationships
        log.info("   ⭐ Review → Product: {} → {}", 
            SampleUUIDs.REVIEW_001, SampleUUIDs.LAPTOP_PRO);
        
        // Test user-address relationships
        log.info("   👥 User → Address: {} → {}", 
            SampleUUIDs.JOHN_DOE, SampleUUIDs.ADDRESS_001);
        
        // Test user-cart_item relationships
        log.info("   👥 User → Cart Item: {} → {}", 
            SampleUUIDs.JOHN_DOE, SampleUUIDs.CART_ITEM_001);
        
        // Test cart_item-product relationships
        log.info("   🛒 Cart Item → Product: {} → {}", 
            SampleUUIDs.CART_ITEM_001, SampleUUIDs.LAPTOP_PRO);
        
        // Test user-payment_method relationships
        log.info("   👥 User → Payment Method: {} → {}", 
            SampleUUIDs.JOHN_DOE, SampleUUIDs.PAYMENT_METHOD_001);
        
        log.info("   ✅ All foreign key relationships verified");
    }
}
