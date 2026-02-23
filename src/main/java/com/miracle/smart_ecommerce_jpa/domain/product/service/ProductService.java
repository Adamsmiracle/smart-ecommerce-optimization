package com.miracle.smart_ecommerce_jpa.domain.product.service;

import com.miracle.smart_ecommerce_jpa.common.response.PageResponse;
import com.miracle.smart_ecommerce_jpa.domain.product.dto.CreateProductRequest;
import com.miracle.smart_ecommerce_jpa.domain.product.dto.ProductResponse;
import com.miracle.smart_ecommerce_jpa.domain.product.dto.UpdateProductRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Service interface for Product operations.
 */
public interface ProductService {

    /**
     * Create a new product
     */
    ProductResponse createProduct(CreateProductRequest request);

    /**
     * Get product by ID
     */
    ProductResponse getProductById(UUID id);

    /**
     * Get all products with pagination
     */
    PageResponse<ProductResponse> getAllProducts(Pageable pageable);

    /**
     * Get active products with pagination
     */
    PageResponse<ProductResponse> getActiveProducts(Pageable pageable);

    /**
     * Get products by category with pagination
     */
    PageResponse<ProductResponse> getProductsByCategory(UUID categoryId, Pageable pageable);

    /**
     * Search products by keyword with pagination
     */
    PageResponse<ProductResponse> searchProducts(String keyword, Pageable pageable);

    /**
     * Get products by price range with pagination
     */
    PageResponse<ProductResponse> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    /**
     * Get products in stock with pagination
     */
    PageResponse<ProductResponse> getProductsInStock(Pageable pageable);

    /**
     * Update product
     */
    ProductResponse updateProduct(UUID id, UpdateProductRequest request);

    /**
     * Delete product
     */
    void deleteProduct(UUID id);

    /**
     * Activate product
     */
    void activateProduct(UUID id);

    /**
     * Deactivate product
     */
    void deactivateProduct(UUID id);

    /**
     * Update product stock quantity
     */
    void updateStock(UUID id, int quantity);

    /**
     * Count total products
     */
    long countProducts();
}