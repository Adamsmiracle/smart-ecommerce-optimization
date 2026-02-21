package com.miracle.smart_ecommerce_jpa.domain.product.repository;

import com.miracle.smart_ecommerce_jpa.domain.product.entity.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Product domain model.
 * Defines data access operations for products.
 */
public interface ProductRepository {

    /**
     * Save a new product
     */
    Product save(Product product);

    /**
     * Update an existing product
     */
    Product update(Product product);

    /**
     * Find product by ID
     */
    Optional<Product> findById(UUID id);


    /**
     * Find all products with pagination
     */
    List<Product> findAll(int page, int size);

    /**
     * Find active products with pagination
     */
    List<Product> findActiveProducts(int page, int size);

    /**
     * Find products by category ID
     */
    List<Product> findByCategoryId(UUID categoryId, int page, int size);

    /**
     * Search products by name or description
     */
    List<Product> search(String keyword, int page, int size);

    /**
     * Find products by price range
     */
    List<Product> findByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, int page, int size);

    /**
     * Find products in stock
     */
    List<Product> findInStock(int page, int size);

    /**
     * Delete product by ID
     */
    void deleteById(UUID id);

    /**
     * Check if product exists by ID
     */
    boolean existsById(UUID id);

    /**
     * Count total products
     */
    long count();

    /**
     * Count active products
     */
    long countActive();

    /**
     * Count products by category
     */
    long countByCategoryId(UUID categoryId);

    /**
     * Update product stock
     */
    void updateStock(UUID productId, int quantity);

    /**
     * Set product active status
     */
    void setActiveStatus(UUID id, boolean isActive);

    /**
     * Batch insert products
     */
    int[] batchInsert(List<Product> products);
}
