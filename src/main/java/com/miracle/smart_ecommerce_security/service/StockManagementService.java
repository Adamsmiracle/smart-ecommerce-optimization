package com.miracle.smart_ecommerce_security.service;

import com.miracle.smart_ecommerce_security.exception.InsufficientStockException;
import com.miracle.smart_ecommerce_security.domain.product.entity.Product;
import com.miracle.smart_ecommerce_security.domain.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Comprehensive stock management service that handles all inventory-related operations
 * and provides detailed insufficient stock exception handling.
 */
@Service
public class StockManagementService {

    private static final Logger log = LoggerFactory.getLogger(StockManagementService.class);
    
    private final ProductRepository productRepository;

    public StockManagementService(ProductRepository productRepository) {
        this.productRepository = productRepository;
        log.info("StockManagementService initialized");
    }

    /**
     * Checks stock availability for a single product
     */
    @Transactional(readOnly = true)
    public StockCheckResult checkStockAvailability(String productId, int requestedQuantity) {
        log.debug("Checking stock availability for product {} - requested: {} - CID: {}", 
            productId, requestedQuantity, MDC.get("correlationId"));
        
        try {
            Product product = productRepository.findById(UUID.fromString(productId))
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
            
            return checkStockAvailability(product, requestedQuantity);
            
        } catch (IllegalArgumentException e) {
            log.error("Product not found during stock check: {} - CID: {}", productId, MDC.get("correlationId"));
            return StockCheckResult.notFound(productId, e.getMessage());
        } catch (Exception e) {
            log.error("Error checking stock availability for product {} - CID: {}", productId, MDC.get("correlationId"), e);
            return StockCheckResult.error(productId, "Error checking stock availability");
        }
    }

    /**
     * Checks stock availability for a product entity
     */
    public StockCheckResult checkStockAvailability(Product product, int requestedQuantity) {
        int available = product.getStockQuantity();
        
        log.debug("Stock check result for product {} - available: {}, requested: {} - CID: {}", 
            product.getId(), available, requestedQuantity, MDC.get("correlationId"));
        
        if (available >= requestedQuantity) {
            return StockCheckResult.available(product.getId().toString(), product.getName(), available, requestedQuantity);
        } else {
            return StockCheckResult.insufficient(product.getId().toString(), product.getName(), available, requestedQuantity);
        }
    }

    /**
     * Checks stock availability for multiple products (bulk operation)
     *
     * Optimizations applied:
     * - DB Queries Reduced: Fetches all required products in 1 DB query using `findAllById` (O(1) queries instead of O(N)).
     * - Hash-Based Lookups: Converts the fetched products into a HashMap, reducing search time complexity from O(N) to O(1) for lookups.
     * Overall time complexity: O(N) to iterate and map products, replacing O(N^2) behavior of sequential lookups.
     */
    @Transactional(readOnly = true)
    public BulkStockCheckResult checkBulkStockAvailability(Map<String, Integer> productRequests) {
        log.info("Checking bulk stock availability for {} products - CID: {}", productRequests.size(), MDC.get("correlationId"));
        
        List<StockCheckResult> results = new ArrayList<>();
        Map<String, InsufficientStockException> insufficientStockExceptions = new HashMap<>();

        try {
            List<UUID> productIds = productRequests.keySet().stream()
                    .map(UUID::fromString)
                    .toList();

            // Optimization: Fetch all products in one database query (Time Complexity: O(1) DB calls)
            List<Product> products = productRepository.findAllById(productIds);

            // Optimization: Hash-based lookup for O(1) access time
            Map<String, Product> productMap = products.stream()
                    .collect(java.util.stream.Collectors.toMap(p -> p.getId().toString(), p -> p));

            for (Map.Entry<String, Integer> entry : productRequests.entrySet()) {
                String productId = entry.getKey();
                int requestedQuantity = entry.getValue();

                try {
                    Product product = productMap.get(productId);
                    if (product == null) {
                        results.add(StockCheckResult.notFound(productId, "Product not found: " + productId));
                        continue;
                    }

                    StockCheckResult result = checkStockAvailability(product, requestedQuantity);
                    results.add(result);

                    if (!result.isAvailable()) {
                        insufficientStockExceptions.put(productId, result.getInsufficientStockException());
                    }

                } catch (Exception e) {
                    log.error("Error checking stock for product {} during bulk operation - CID: {}", productId, MDC.get("correlationId"), e);
                    results.add(StockCheckResult.error(productId, e.getMessage()));
                }
            }
        } catch (Exception e) {
             log.error("Error initializing bulk check - CID: {}", MDC.get("correlationId"), e);
             return new BulkStockCheckResult(false, results, insufficientStockExceptions);
        }
        
        boolean allAvailable = results.stream().allMatch(StockCheckResult::isAvailable);
        
        log.info("Bulk stock check completed - all available: {}, insufficient products: {} - CID: {}", 
            allAvailable, insufficientStockExceptions.size(), MDC.get("correlationId"));
        
        return new BulkStockCheckResult(allAvailable, results, insufficientStockExceptions);
    }

    /**
     * Validates stock for cart operations
     */
    @Transactional(readOnly = true)
    public void validateCartStock(Map<String, Integer> cartItems) throws InsufficientStockException {
        log.debug("Validating cart stock for {} items - CID: {}", cartItems.size(), MDC.get("correlationId"));
        
        BulkStockCheckResult bulkResult = checkBulkStockAvailability(cartItems);
        
        if (!bulkResult.isAllAvailable()) {
            // Throw the first insufficient stock exception found
            InsufficientStockException firstException = bulkResult.getInsufficientStockExceptions().values().iterator().next();
            log.warn("Cart stock validation failed - {} - CID: {}", firstException.getMessage(), MDC.get("correlationId"));
            throw firstException;
        }
        
        log.debug("Cart stock validation passed - CID: {}", MDC.get("correlationId"));
    }

    /**
     * Validates stock for order creation
     */
    @Transactional(readOnly = true)
    public void validateOrderStock(Map<String, Integer> orderItems, String orderId) throws InsufficientStockException {
        log.debug("Validating order stock for {} items - orderId: {} - CID: {}", orderItems.size(), orderId, MDC.get("correlationId"));
        
        BulkStockCheckResult bulkResult = checkBulkStockAvailability(orderItems);
        
        if (!bulkResult.isAllAvailable()) {
            // Create enhanced exception with order context
            Map<String, InsufficientStockException> exceptions = bulkResult.getInsufficientStockExceptions();
            InsufficientStockException firstException = exceptions.values().iterator().next();
            
            // Create new exception with order context
            InsufficientStockException orderException = InsufficientStockException.forOrderCreation(
                firstException.getProductId(), 
                firstException.getProductName(), 
                firstException.getAvailable(), 
                firstException.getRequested(), 
                orderId
            );
            
            log.warn("Order stock validation failed - orderId: {}, product: {} - CID: {}", 
                orderId, firstException.getProductId(), MDC.get("correlationId"));
            throw orderException;
        }
        
        log.debug("Order stock validation passed - orderId: {} - CID: {}", orderId, MDC.get("correlationId"));
    }

    /**
     * Reserves stock for order processing
     *
     * Optimizations applied:
     * - O(1) DB calls to retrieve all required products.
     * - Saves all updated products concurrently utilizing `saveAll`, saving execution time over iterating `save`.
     */
    @Transactional
    public StockReservationResult reserveStock(Map<String, Integer> orderItems, String orderId) {
        log.info("Reserving stock for order {} with {} items - CID: {}", orderId, orderItems.size(), MDC.get("correlationId"));
        
        List<StockReservation> reservations = new ArrayList<>();
        
        try {
            // First validate all stock is available
            validateOrderStock(orderItems, orderId);
            
            // Optimization: Fetch all products in one DB query
            List<UUID> productIds = orderItems.keySet().stream()
                    .map(UUID::fromString)
                    .toList();
            List<Product> products = productRepository.findAllById(productIds);

            // Optimization: O(1) Hash-based search memory mapping
            Map<String, Product> productMap = products.stream()
                    .collect(java.util.stream.Collectors.toMap(p -> p.getId().toString(), p -> p));

            // Reserve stock for each item
            for (Map.Entry<String, Integer> entry : orderItems.entrySet()) {
                String productId = entry.getKey();
                int quantity = entry.getValue();
                
                Product product = productMap.get(productId);
                if (product == null) {
                    throw new IllegalArgumentException("Product not found: " + productId);
                }

                int currentStock = product.getStockQuantity();
                int newStock = currentStock - quantity;
                
                if (newStock < 0) {
                    throw InsufficientStockException.forOrderCreation(productId, product.getName(), currentStock, quantity, orderId);
                }
                
                product.setStockQuantity(newStock);

                reservations.add(new StockReservation(productId, product.getName(), quantity, currentStock, newStock));
                
                log.debug("Stock reserved - product: {}, quantity: {}, old stock: {}, new stock: {} - CID: {}", 
                    productId, quantity, currentStock, newStock, MDC.get("correlationId"));
            }
            
            // Save modified items
            productRepository.saveAll(products);

            log.info("Stock reservation completed for order {} - {} items reserved - CID: {}",
                orderId, reservations.size(), MDC.get("correlationId"));
            
            return new StockReservationResult(true, reservations, null);
            
        } catch (InsufficientStockException e) {
            log.warn("Stock reservation failed for order {} - {} - CID: {}", orderId, e.getMessage(), MDC.get("correlationId"));
            return new StockReservationResult(false, null, e);
        } catch (Exception e) {
            log.error("Unexpected error during stock reservation for order {} - CID: {}", orderId, MDC.get("correlationId"), e);
            return new StockReservationResult(false, null, new RuntimeException("Stock reservation failed", e));
        }
    }

    /**
     * Releases reserved stock (for order cancellations)
     *
     * Optimizations applied:
     * - O(1) Fetch DB calls and `saveAll` processing.
     */
    @Transactional
    public void releaseReservedStock(Map<String, Integer> orderItems, String orderId) {
        log.info("Releasing reserved stock for order {} with {} items - CID: {}", orderId, orderItems.size(), MDC.get("correlationId"));
        
        List<UUID> productIds = orderItems.keySet().stream()
                .map(id -> {
                    try {
                        return UUID.fromString(id);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(id -> id != null)
                .toList();

        List<Product> productsToSave = new ArrayList<>();

        try {
            List<Product> products = productRepository.findAllById(productIds);
            Map<String, Product> productMap = products.stream()
                    .collect(java.util.stream.Collectors.toMap(p -> p.getId().toString(), p -> p));

            for (Map.Entry<String, Integer> entry : orderItems.entrySet()) {
                String productId = entry.getKey();
                int quantity = entry.getValue();

                try {
                    Product product = productMap.get(productId);
                    if (product == null) {
                        log.error("Product not found: {}", productId);
                        continue;
                    }

                    int currentStock = product.getStockQuantity();
                    int newStock = currentStock + quantity;

                    product.setStockQuantity(newStock);
                    productsToSave.add(product);

                    log.debug("Stock released - product: {}, quantity: {}, old stock: {}, new stock: {} - CID: {}",
                        productId, quantity, currentStock, newStock, MDC.get("correlationId"));

                } catch (Exception e) {
                    log.error("Error releasing stock for product {} in order {} - CID: {}", productId, orderId, MDC.get("correlationId"), e);
                    // Continue with other items even if one fails
                }
            }

            if (!productsToSave.isEmpty()) {
                productRepository.saveAll(productsToSave);
            }
        } catch (Exception e) {
             log.error("Error releasing stock for order {} - CID: {}", orderId, MDC.get("correlationId"), e);
        }
        
        log.info("Stock release completed for order {} - CID: {}", orderId, MDC.get("correlationId"));
    }

    /**
     * Gets low stock alerts
     */
    @Transactional(readOnly = true)
    public List<LowStockAlert> getLowStockAlerts(int threshold) {
        log.debug("Checking for low stock alerts with threshold {} - CID: {}", threshold, MDC.get("correlationId"));
        
        // Use parallel stream to process products for low stock alerts concurrently
        List<Product> allProducts = productRepository.findAll();

        List<LowStockAlert> alerts = allProducts.parallelStream()
                .filter(product -> product.getStockQuantity() <= threshold && Boolean.TRUE.equals(product.getIsActive()))
                .map(product -> new LowStockAlert(
                    product.getId().toString(),
                    product.getName(),
                    product.getStockQuantity(),
                    threshold,
                    product.getStockQuantity() <= 0
                ))
                .toList();

        log.info("Found {} low stock alerts - CID: {}", alerts.size(), MDC.get("correlationId"));
        return alerts;
    }

    // Result classes
    public static class StockCheckResult {
        private final String productId;
        private final String productName;
        private final int availableQuantity;
        private final int requestedQuantity;
        private final boolean availableFlag;
        private final InsufficientStockException insufficientStockException;
        private final String error;

        private StockCheckResult(String productId, String productName, int availableQuantity, int requestedQuantity,
                               boolean availableFlag, InsufficientStockException insufficientStockException, String error) {
            this.productId = productId;
            this.productName = productName;
            this.availableQuantity = availableQuantity;
            this.requestedQuantity = requestedQuantity;
            this.availableFlag = availableFlag;
            this.insufficientStockException = insufficientStockException;
            this.error = error;
        }

        public static StockCheckResult available(String productId, String productName, int available, int requested) {
            return new StockCheckResult(productId, productName, available, requested, true, null, null);
        }

        public static StockCheckResult insufficient(String productId, String productName, int available, int requested) {
            InsufficientStockException exception = InsufficientStockException.forProduct(productId, productName, available, requested);
            return new StockCheckResult(productId, productName, available, requested, false, exception, null);
        }

        public static StockCheckResult notFound(String productId, String error) {
            return new StockCheckResult(productId, null, 0, 0, false, null, error);
        }

        public static StockCheckResult error(String productId, String error) {
            return new StockCheckResult(productId, null, 0, 0, false, null, error);
        }

        // Getters
        public String getProductId() { return productId; }
        public String getProductName() { return productName; }
        public int getAvailable() { return availableQuantity; }
        public int getRequested() { return requestedQuantity; }
        public boolean isAvailable() { return availableFlag; }
        public InsufficientStockException getInsufficientStockException() { return insufficientStockException; }
        public String getError() { return error; }
    }

    public static class BulkStockCheckResult {
        private final boolean allAvailable;
        private final List<StockCheckResult> results;
        private final Map<String, InsufficientStockException> insufficientStockExceptions;

        public BulkStockCheckResult(boolean allAvailable, List<StockCheckResult> results, 
                                   Map<String, InsufficientStockException> insufficientStockExceptions) {
            this.allAvailable = allAvailable;
            this.results = results;
            this.insufficientStockExceptions = insufficientStockExceptions;
        }

        public boolean isAllAvailable() { return allAvailable; }
        public List<StockCheckResult> getResults() { return results; }
        public Map<String, InsufficientStockException> getInsufficientStockExceptions() { return insufficientStockExceptions; }
    }

    public static class StockReservation {
        private final String productId;
        private final String productName;
        private final int quantity;
        private final int previousStock;
        private final int newStock;

        public StockReservation(String productId, String productName, int quantity, int previousStock, int newStock) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.previousStock = previousStock;
            this.newStock = newStock;
        }

        // Getters
        public String getProductId() { return productId; }
        public String getProductName() { return productName; }
        public int getQuantity() { return quantity; }
        public int getPreviousStock() { return previousStock; }
        public int getNewStock() { return newStock; }
    }

    public static class StockReservationResult {
        private final boolean success;
        private final List<StockReservation> reservations;
        private final Exception exception;

        public StockReservationResult(boolean success, List<StockReservation> reservations, Exception exception) {
            this.success = success;
            this.reservations = reservations;
            this.exception = exception;
        }

        public boolean isSuccess() { return success; }
        public List<StockReservation> getReservations() { return reservations; }
        public Exception getException() { return exception; }
    }

    public static class LowStockAlert {
        private final String productId;
        private final String productName;
        private final int currentStock;
        private final int threshold;
        private final boolean outOfStock;

        public LowStockAlert(String productId, String productName, int currentStock, int threshold, boolean outOfStock) {
            this.productId = productId;
            this.productName = productName;
            this.currentStock = currentStock;
            this.threshold = threshold;
            this.outOfStock = outOfStock;
        }

        // Getters
        public String getProductId() { return productId; }
        public String getProductName() { return productName; }
        public int getCurrentStock() { return currentStock; }
        public int getThreshold() { return threshold; }
        public boolean isOutOfStock() { return outOfStock; }
    }
}
