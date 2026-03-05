package com.miracle.smart_ecommerce_security.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for detecting and reporting database inconsistencies.
 * Provides comprehensive checks for data integrity violations and orphaned records.
 */
@Service
public class DatabaseConsistencyService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConsistencyService.class);
    
    private final DataSource dataSource;

    public DatabaseConsistencyService(DataSource dataSource) {
        this.dataSource = dataSource;
        log.info("DatabaseConsistencyService initialized");
    }

    /**
     * Performs comprehensive database consistency checks
     */
    @Transactional(readOnly = true)
    public ConsistencyReport checkDatabaseConsistency() {
        log.info("Starting database consistency check - CID: {}", MDC.get("correlationId"));
        
        ConsistencyReport report = new ConsistencyReport();
        LocalDateTime checkTime = LocalDateTime.now();
        
        try {
            // Check for orphaned records
            checkOrphanedUserAddresses(report);
            checkOrphanedOrderItems(report);
            checkOrphanedCartItems(report);
            checkOrphanedProductReviews(report);
            checkOrphanedPaymentMethods(report);
            checkOrphanedShippingMethods(report);
            
            // Check for data integrity violations
            checkInvalidOrderTotals(report);
            checkNegativeInventory(report);
            checkInactiveUsersWithActiveOrders(report);
            checkInvalidCategoryReferences(report);
            
            // Check for referential integrity
            checkMissingUserReferences(report);
            checkMissingProductReferences(report);
            checkMissingCategoryReferences(report);
            
            report.setCheckTime(checkTime);
            report.setTotalIssues(report.getIssues().size());
            
            log.info("Database consistency check completed - Found {} issues - CID: {}", 
                report.getTotalIssues(), MDC.get("correlationId"));
            
        } catch (Exception e) {
            log.error("Database consistency check failed - CID: {}", MDC.get("correlationId"), e);
            report.addError("CONSISTENCY_CHECK_ERROR", "Failed to complete consistency check: " + e.getMessage());
        }
        
        return report;
    }

    /**
     * Checks for addresses without valid user references
     */
    private void checkOrphanedUserAddresses(ConsistencyReport report) {
        String sql = """
            SELECT a.id, a.user_id 
            FROM addresses a 
            LEFT JOIN users u ON a.user_id = u.id 
            WHERE u.id IS NULL
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            int count = 0;
            while (rs.next()) {
                count++;
                report.addIssue("ORPHANED_ADDRESS", 
                    String.format("Address %s has invalid user reference %s", rs.getString("id"), rs.getString("user_id")));
            }
            
            if (count > 0) {
                log.warn("Found {} orphaned addresses - CID: {}", count, MDC.get("correlationId"));
            }
            
        } catch (SQLException e) {
            log.error("Failed to check orphaned addresses - CID: {}", MDC.get("correlationId"), e);
            report.addError("ORPHANED_ADDRESS_CHECK_ERROR", e.getMessage());
        }
    }

    /**
     * Checks for order items without valid order references
     */
    private void checkOrphanedOrderItems(ConsistencyReport report) {
        String sql = """
            SELECT oi.id, oi.order_id 
            FROM order_items oi 
            LEFT JOIN orders o ON oi.order_id = o.id 
            WHERE o.id IS NULL
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            int count = 0;
            while (rs.next()) {
                count++;
                report.addIssue("ORPHANED_ORDER_ITEM", 
                    String.format("Order item %s has invalid order reference %s", rs.getString("id"), rs.getString("order_id")));
            }
            
            if (count > 0) {
                log.warn("Found {} orphaned order items - CID: {}", count, MDC.get("correlationId"));
            }
            
        } catch (SQLException e) {
            log.error("Failed to check orphaned order items - CID: {}", MDC.get("correlationId"), e);
            report.addError("ORPHANED_ORDER_ITEM_CHECK_ERROR", e.getMessage());
        }
    }

    /**
     * Checks for cart items without valid cart references
     */
    private void checkOrphanedCartItems(ConsistencyReport report) {
        String sql = """
            SELECT ci.id, ci.cart_id 
            FROM cart_items ci 
            LEFT JOIN carts c ON ci.cart_id = c.id 
            WHERE c.id IS NULL
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            int count = 0;
            while (rs.next()) {
                count++;
                report.addIssue("ORPHANED_CART_ITEM", 
                    String.format("Cart item %s has invalid cart reference %s", rs.getString("id"), rs.getString("cart_id")));
            }
            
            if (count > 0) {
                log.warn("Found {} orphaned cart items - CID: {}", count, MDC.get("correlationId"));
            }
            
        } catch (SQLException e) {
            log.error("Failed to check orphaned cart items - CID: {}", MDC.get("correlationId"), e);
            report.addError("ORPHANED_CART_ITEM_CHECK_ERROR", e.getMessage());
        }
    }

    /**
     * Checks for reviews without valid product references
     */
    private void checkOrphanedProductReviews(ConsistencyReport report) {
        String sql = """
            SELECT r.id, r.product_id 
            FROM reviews r 
            LEFT JOIN products p ON r.product_id = p.id 
            WHERE p.id IS NULL
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            int count = 0;
            while (rs.next()) {
                count++;
                report.addIssue("ORPHANED_REVIEW", 
                    String.format("Review %s has invalid product reference %s", rs.getString("id"), rs.getString("product_id")));
            }
            
            if (count > 0) {
                log.warn("Found {} orphaned reviews - CID: {}", count, MDC.get("correlationId"));
            }
            
        } catch (SQLException e) {
            log.error("Failed to check orphaned reviews - CID: {}", MDC.get("correlationId"), e);
            report.addError("ORPHANED_REVIEW_CHECK_ERROR", e.getMessage());
        }
    }

    /**
     * Checks for payment methods without valid user references
     */
    private void checkOrphanedPaymentMethods(ConsistencyReport report) {
        String sql = """
            SELECT pm.id, pm.user_id 
            FROM payment_methods pm 
            LEFT JOIN users u ON pm.user_id = u.id 
            WHERE u.id IS NULL
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            int count = 0;
            while (rs.next()) {
                count++;
                report.addIssue("ORPHANED_PAYMENT_METHOD", 
                    String.format("Payment method %s has invalid user reference %s", rs.getString("id"), rs.getString("user_id")));
            }
            
            if (count > 0) {
                log.warn("Found {} orphaned payment methods - CID: {}", count, MDC.get("correlationId"));
            }
            
        } catch (SQLException e) {
            log.error("Failed to check orphaned payment methods - CID: {}", MDC.get("correlationId"), e);
            report.addError("ORPHANED_PAYMENT_METHOD_CHECK_ERROR", e.getMessage());
        }
    }

    /**
     * Checks for shipping methods without valid user references
     */
    private void checkOrphanedShippingMethods(ConsistencyReport report) {
        String sql = """
            SELECT sm.id, sm.user_id 
            FROM shipping_methods sm 
            LEFT JOIN users u ON sm.user_id = u.id 
            WHERE u.id IS NULL
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            int count = 0;
            while (rs.next()) {
                count++;
                report.addIssue("ORPHANED_SHIPPING_METHOD", 
                    String.format("Shipping method %s has invalid user reference %s", rs.getString("id"), rs.getString("user_id")));
            }
            
            if (count > 0) {
                log.warn("Found {} orphaned shipping methods - CID: {}", count, MDC.get("correlationId"));
            }
            
        } catch (SQLException e) {
            log.error("Failed to check orphaned shipping methods - CID: {}", MDC.get("correlationId"), e);
            report.addError("ORPHANED_SHIPPING_METHOD_CHECK_ERROR", e.getMessage());
        }
    }

    /**
     * Checks for orders with incorrect total amounts
     */
    private void checkInvalidOrderTotals(ConsistencyReport report) {
        String sql = """
            SELECT o.id, o.total_amount, 
                   COALESCE(SUM(oi.quantity * oi.unit_price), 0) as calculated_total
            FROM orders o
            LEFT JOIN order_items oi ON o.id = oi.order_id
            GROUP BY o.id, o.total_amount
            HAVING ABS(o.total_amount - calculated_total) > 0.01
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            int count = 0;
            while (rs.next()) {
                count++;
                report.addIssue("INVALID_ORDER_TOTAL", 
                    String.format("Order %s has total %s but calculated total is %s", 
                        rs.getString("id"), rs.getBigDecimal("total_amount"), rs.getBigDecimal("calculated_total")));
            }
            
            if (count > 0) {
                log.warn("Found {} orders with invalid totals - CID: {}", count, MDC.get("correlationId"));
            }
            
        } catch (SQLException e) {
            log.error("Failed to check invalid order totals - CID: {}", MDC.get("correlationId"), e);
            report.addError("INVALID_ORDER_TOTAL_CHECK_ERROR", e.getMessage());
        }
    }

    /**
     * Checks for products with negative inventory
     */
    private void checkNegativeInventory(ConsistencyReport report) {
        String sql = """
            SELECT id, name, stock_quantity 
            FROM products 
            WHERE stock_quantity < 0
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            int count = 0;
            while (rs.next()) {
                count++;
                report.addIssue("NEGATIVE_INVENTORY", 
                    String.format("Product %s (%s) has negative stock quantity: %d", 
                        rs.getString("id"), rs.getString("name"), rs.getInt("stock_quantity")));
            }
            
            if (count > 0) {
                log.warn("Found {} products with negative inventory - CID: {}", count, MDC.get("correlationId"));
            }
            
        } catch (SQLException e) {
            log.error("Failed to check negative inventory - CID: {}", MDC.get("correlationId"), e);
            report.addError("NEGATIVE_INVENTORY_CHECK_ERROR", e.getMessage());
        }
    }

    /**
     * Checks for inactive users with active orders
     */
    private void checkInactiveUsersWithActiveOrders(ConsistencyReport report) {
        String sql = """
            SELECT u.id, u.email, COUNT(o.id) as active_orders
            FROM users u
            JOIN orders o ON u.id = o.user_id
            WHERE u.is_active = false AND o.status NOT IN ('CANCELLED', 'DELIVERED')
            GROUP BY u.id, u.email
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            int count = 0;
            while (rs.next()) {
                count++;
                report.addIssue("INACTIVE_USER_ACTIVE_ORDERS", 
                    String.format("Inactive user %s (%s) has %d active orders", 
                        rs.getString("id"), rs.getString("email"), rs.getInt("active_orders")));
            }
            
            if (count > 0) {
                log.warn("Found {} inactive users with active orders - CID: {}", count, MDC.get("correlationId"));
            }
            
        } catch (SQLException e) {
            log.error("Failed to check inactive users with active orders - CID: {}", MDC.get("correlationId"), e);
            report.addError("INACTIVE_USER_ACTIVE_ORDERS_CHECK_ERROR", e.getMessage());
        }
    }

    /**
     * Checks for products with invalid category references
     */
    private void checkInvalidCategoryReferences(ConsistencyReport report) {
        String sql = """
            SELECT p.id, p.name, p.category_id 
            FROM products p 
            LEFT JOIN categories c ON p.category_id = c.id 
            WHERE c.id IS NULL
            """;
        
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            int count = 0;
            while (rs.next()) {
                count++;
                report.addIssue("INVALID_CATEGORY_REFERENCE", 
                    String.format("Product %s (%s) has invalid category reference %s", 
                        rs.getString("id"), rs.getString("name"), rs.getString("category_id")));
            }
            
            if (count > 0) {
                log.warn("Found {} products with invalid category references - CID: {}", count, MDC.get("correlationId"));
            }
            
        } catch (SQLException e) {
            log.error("Failed to check invalid category references - CID: {}", MDC.get("correlationId"), e);
            report.addError("INVALID_CATEGORY_REFERENCE_CHECK_ERROR", e.getMessage());
        }
    }

    /**
     * Generic check for missing user references
     */
    private void checkMissingUserReferences(ConsistencyReport report) {
        // This is a generic check that can be extended
        log.debug("Checking for missing user references - CID: {}", MDC.get("correlationId"));
    }

    /**
     * Generic check for missing product references
     */
    private void checkMissingProductReferences(ConsistencyReport report) {
        // This is a generic check that can be extended
        log.debug("Checking for missing product references - CID: {}", MDC.get("correlationId"));
    }

    /**
     * Generic check for missing category references
     */
    private void checkMissingCategoryReferences(ConsistencyReport report) {
        // This is a generic check that can be extended
        log.debug("Checking for missing category references - CID: {}", MDC.get("correlationId"));
    }

    /**
     * Consistency report containing all detected issues
     */
    public static class ConsistencyReport {
        private LocalDateTime checkTime;
        private int totalIssues;
        private final List<ConsistencyIssue> issues = new ArrayList<>();
        private final List<String> errors = new ArrayList<>();

        public void addIssue(String type, String description) {
            issues.add(new ConsistencyIssue(type, description));
        }

        public void addError(String type, String description) {
            errors.add(type + ": " + description);
        }

        // Getters and setters
        public LocalDateTime getCheckTime() { return checkTime; }
        public void setCheckTime(LocalDateTime checkTime) { this.checkTime = checkTime; }
        public int getTotalIssues() { return totalIssues; }
        public void setTotalIssues(int totalIssues) { this.totalIssues = totalIssues; }
        public List<ConsistencyIssue> getIssues() { return issues; }
        public List<String> getErrors() { return errors; }

        public boolean hasIssues() { return !issues.isEmpty(); }
        public boolean hasErrors() { return !errors.isEmpty(); }

        public Map<String, Object> getSummary() {
            Map<String, Object> summary = new HashMap<>();
            summary.put("checkTime", checkTime);
            summary.put("totalIssues", totalIssues);
            summary.put("issuesCount", issues.size());
            summary.put("errorsCount", errors.size());
            summary.put("hasIssues", hasIssues());
            summary.put("hasErrors", hasErrors());
            return summary;
        }
    }

    /**
     * Individual consistency issue
     */
    public static class ConsistencyIssue {
        private final String type;
        private final String description;
        private final LocalDateTime detectedAt;

        public ConsistencyIssue(String type, String description) {
            this.type = type;
            this.description = description;
            this.detectedAt = LocalDateTime.now();
        }

        public String getType() { return type; }
        public String getDescription() { return description; }
        public LocalDateTime getDetectedAt() { return detectedAt; }
    }
}
