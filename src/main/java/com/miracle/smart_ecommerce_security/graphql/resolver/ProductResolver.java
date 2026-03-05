package com.miracle.smart_ecommerce_security.graphql.resolver;

import com.miracle.smart_ecommerce_security.domain.product.dto.CreateProductRequest;
import com.miracle.smart_ecommerce_security.domain.product.dto.ProductResponse;
import com.miracle.smart_ecommerce_security.domain.product.dto.UpdateProductRequest;
import com.miracle.smart_ecommerce_security.domain.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * GraphQL Resolver for Product entity.
 * Handles all product-related queries and mutations.
 *
 * Access: Queries open to ADMIN + CUSTOMER; mutations restricted to ADMIN.
 */
@Controller
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER')")
public class ProductResolver {

    private final ProductService productService;

    // ========================================================================
    // PRODUCT QUERIES
    // ========================================================================

    @QueryMapping
    public ProductResponse product(@Argument UUID id) {
        return productService.getProductById(id);
    }

    @QueryMapping
    public Page<ProductResponse> products(@Argument int page, @Argument int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productService.getAllProducts(pageable);
    }

    @QueryMapping
    public Page<ProductResponse> activeProducts(@Argument int page, @Argument int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productService.getActiveProducts(pageable);
    }

    @QueryMapping
    public Page<ProductResponse> productsByCategory(@Argument UUID categoryId,
                                                    @Argument int page,
                                                    @Argument int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productService.getProductsByCategory(categoryId, pageable);
    }

    @QueryMapping
    public Page<ProductResponse> searchProducts(@Argument String keyword,
                                                @Argument int page,
                                                @Argument int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productService.searchProducts(keyword, pageable);
    }

    @QueryMapping
    public Page<ProductResponse> productsInStock(@Argument int page, @Argument int size) {
        Pageable pageable = PageRequest.of(page, size);
        return productService.getProductsInStock(pageable);
    }

    // ========================================================================
    // PRODUCT MUTATIONS
    // ========================================================================

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ProductResponse createProduct(@Argument Map<String, Object> input) {
        CreateProductRequest request = mapToProductRequest(input);
        return productService.createProduct(request);
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ProductResponse updateProduct(@Argument UUID id, @Argument Map<String, Object> input) {
        UpdateProductRequest request = mapToUpdateProductRequest(input);
        return productService.updateProduct(id, request);
    }

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    public boolean deleteProduct(@Argument UUID id) {
        productService.deleteProduct(id);
        return true;
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public boolean activateProduct(@Argument UUID id) {
        productService.activateProduct(id);
        return true;
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public boolean deactivateProduct(@Argument UUID id) {
        productService.deactivateProduct(id);
        return true;
    }

    @MutationMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
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
