package com.miracle.smart_ecommerce_jpa.domain.product.service;

import com.miracle.smart_ecommerce_jpa.common.response.PageResponse;
import com.miracle.smart_ecommerce_jpa.domain.product.dto.CreateProductRequest;
import com.miracle.smart_ecommerce_jpa.domain.product.dto.ProductResponse;

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
     * Get product by SKU
     */
    ProductResponse getProductBySku(String sku);

    /**
     * Get all products with pagination
     */
    PageResponse<ProductResponse> getAllProducts(int page, int size);

    /**
     * Get active products with pagination
     */
    PageResponse<ProductResponse> getActiveProducts(int page, int size);

    /**
     * Get products by category
     */
    PageResponse<ProductResponse> getProductsByCategory(UUID categoryId, int page, int size);

    /**
     * Search products by keyword
     */
    PageResponse<ProductResponse> searchProducts(String keyword, int page, int size);

    /**
     * Get products by price range
     */
    PageResponse<ProductResponse> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, int page, int size);

    /**
     * Get products in stock
     */
    PageResponse<ProductResponse> getProductsInStock(int page, int size);

    /**
     * Update product
     */
    ProductResponse updateProduct(UUID id, CreateProductRequest request);
    ProductResponse updateProduct(UUID id, com.miracle.smart_ecommerce_jpa.domain.product.dto.UpdateProductRequest request);

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
     * Update product stock
     */
    void updateStock(UUID id, int quantity);

    /**
     * Count total products
     */
    long countProducts();
}
