package com.miracle.smart_ecommerce_jpa.graphql.resolver;

import com.miracle.smart_ecommerce_jpa.annotation.RequireRoles;
import com.miracle.smart_ecommerce_jpa.common.response.PageResponse;
import com.miracle.smart_ecommerce_jpa.domain.product.dto.CreateProductRequest;
import com.miracle.smart_ecommerce_jpa.domain.product.dto.ProductResponse;
import com.miracle.smart_ecommerce_jpa.domain.product.dto.UpdateProductRequest;
import com.miracle.smart_ecommerce_jpa.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * GraphQL Resolver for Product entity.
 * Handles all product-related queries and mutations.
 */
@Controller
@RequiredArgsConstructor
@RequireRoles({"ADMIN", "CUSTOMER"})
public class ProductResolver {

    private final ProductService productService;

    // ========================================================================
    // PRODUCT QUERIES
    // ========================================================================

    @QueryMapping
    @RequireRoles({"ADMIN", "CUSTOMER"})
    public ProductResponse product(@Argument UUID id) {
        return productService.getProductById(id);
    }

    @QueryMapping
    @RequireRoles({"ADMIN", "CUSTOMER"})
    public PageResponse<ProductResponse> products(@Argument int page, @Argument int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productService.getAllProducts(pageable);
    }

    @QueryMapping
    @RequireRoles({"ADMIN", "CUSTOMER"})
    public PageResponse<ProductResponse> activeProducts(@Argument int page, @Argument int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productService.getActiveProducts(pageable);
    }

    @QueryMapping
    @RequireRoles({"ADMIN", "CUSTOMER"})
    public PageResponse<ProductResponse> productsByCategory(@Argument UUID categoryId,
                                                            @Argument int page,
                                                            @Argument int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productService.getProductsByCategory(categoryId, pageable);
    }

    @QueryMapping
    @RequireRoles({"ADMIN", "CUSTOMER"})
    public PageResponse<ProductResponse> searchProducts(@Argument String keyword,
                                                        @Argument int page,
                                                        @Argument int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productService.searchProducts(keyword, pageable);
    }

    @QueryMapping
    @RequireRoles({"ADMIN", "CUSTOMER"})
    public PageResponse<ProductResponse> productsInStock(@Argument int page, @Argument int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productService.getProductsInStock(pageable);
    }

    // ========================================================================
    // PRODUCT MUTATIONS
    // ========================================================================

    @MutationMapping
    @RequireRoles({"ADMIN"})
    public ProductResponse createProduct(@Argument Map<String, Object> input) {
        CreateProductRequest request = mapToProductRequest(input);
        return productService.createProduct(request);
    }

    @MutationMapping
    @RequireRoles({"ADMIN"})
    public ProductResponse updateProduct(@Argument UUID id, @Argument Map<String, Object> input) {
        UpdateProductRequest request = mapToUpdateProductRequest(input);
        return productService.updateProduct(id, request);
    }

    @MutationMapping
    @RequireRoles({"ADMIN"})
    public boolean deleteProduct(@Argument UUID id) {
        productService.deleteProduct(id);
        return true;
    }

    @MutationMapping
    @RequireRoles({"ADMIN"})
    public boolean activateProduct(@Argument UUID id) {
        productService.activateProduct(id);
        return true;
    }

    @MutationMapping
    @RequireRoles({"ADMIN"})
    public boolean deactivateProduct(@Argument UUID id) {
        productService.deactivateProduct(id);
        return true;
    }

    @MutationMapping
    @RequireRoles({"ADMIN"})
    public boolean updateStock(@Argument UUID id, @Argument int quantity) {
        productService.updateStock(id, quantity);
        return true;
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    @SuppressWarnings("unchecked")
    private CreateProductRequest mapToProductRequest(Map<String, Object> input) {
        return CreateProductRequest.builder()
                .categoryId(input.get("categoryId") != null
                        ? UUID.fromString((String) input.get("categoryId")) : null)
                .name((String) input.get("name"))
                .description((String) input.get("description"))
                .price(input.get("price") != null
                        ? new BigDecimal(input.get("price").toString()) : null)
                .stockQuantity(input.get("stockQuantity") != null
                        ? (Integer) input.get("stockQuantity") : null)
                .isActive(input.get("isActive") != null
                        ? (Boolean) input.get("isActive") : null)
                .images(input.get("images") != null
                        ? (List<String>) input.get("images") : null)
                .build();
    }

    @SuppressWarnings("unchecked")
    private UpdateProductRequest mapToUpdateProductRequest(Map<String, Object> input) {
        return UpdateProductRequest.builder()
                .categoryId(input.get("categoryId") != null
                        ? UUID.fromString((String) input.get("categoryId")) : null)
                .name((String) input.get("name"))
                .description((String) input.get("description"))
                .price(input.get("price") != null
                        ? new BigDecimal(input.get("price").toString()) : null)
                .stockQuantity(input.get("stockQuantity") != null
                        ? (Integer) input.get("stockQuantity") : null)
                .isActive(input.get("isActive") != null
                        ? (Boolean) input.get("isActive") : null)
                .images(input.get("images") != null
                        ? (List<String>) input.get("images") : null)
                .build();
    }
}

