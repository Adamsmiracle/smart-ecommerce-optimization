package com.miracle.smart_ecommerce_jpa.domain.product.controller;

import com.miracle.smart_ecommerce_jpa.common.response.ApiResponse;
import com.miracle.smart_ecommerce_jpa.common.response.PageResponse;
import com.miracle.smart_ecommerce_jpa.domain.product.dto.CreateProductRequest;
import com.miracle.smart_ecommerce_jpa.domain.product.dto.ProductResponse;
import com.miracle.smart_ecommerce_jpa.domain.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * REST Controller for Product management.
 */
@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Product management APIs")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @Operation(summary = "Create a new product", description = "Creates a new product")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Product created successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid input"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category not found")
    })
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductResponse product = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.created(product, "Product created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID", description = "Retrieves a product by its unique ID")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(
            @Parameter(description = "Product ID") @PathVariable UUID id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @GetMapping("/sku/{sku}")
    @Operation(summary = "Get product by SKU", description = "Retrieves a product by its SKU")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductBySku(
            @Parameter(description = "Product SKU") @PathVariable String sku) {
        ProductResponse product = productService.getProductBySku(sku);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @GetMapping
    @Operation(summary = "Get all products", description = "Retrieves all products with pagination")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getAllProducts(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        PageResponse<ProductResponse> products = productService.getAllProducts(page, size);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/active")
    @Operation(summary = "Get active products", description = "Retrieves active products with pagination")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getActiveProducts(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        PageResponse<ProductResponse> products = productService.getActiveProducts(page, size);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "Get products by category", description = "Retrieves products by category ID")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProductsByCategory(
            @Parameter(description = "Category ID") @PathVariable UUID categoryId,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        PageResponse<ProductResponse> products = productService.getProductsByCategory(categoryId, page, size);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/search")
    @Operation(summary = "Search products", description = "Search products by keyword")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> searchProducts(
            @Parameter(description = "Search keyword") @RequestParam String keyword,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        PageResponse<ProductResponse> products = productService.searchProducts(keyword, page, size);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/price-range")
    @Operation(summary = "Get products by price range", description = "Retrieves products within a price range")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProductsByPriceRange(
            @Parameter(description = "Minimum price") @RequestParam BigDecimal minPrice,
            @Parameter(description = "Maximum price") @RequestParam BigDecimal maxPrice,
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        PageResponse<ProductResponse> products = productService.getProductsByPriceRange(minPrice, maxPrice, page, size);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/in-stock")
    @Operation(summary = "Get products in stock", description = "Retrieves products that are in stock")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProductsInStock(
            @Parameter(description = "Page number (0-based)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "10") int size) {
        PageResponse<ProductResponse> products = productService.getProductsInStock(page, size);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product", description = "Updates an existing product")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @Parameter(description = "Product ID") @PathVariable UUID id,
            @RequestBody com.miracle.smart_ecommerce_jpa.domain.product.dto.UpdateProductRequest request) {
        ProductResponse product = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success(product, "Product updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete product", description = "Deletes a product by ID")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(
            @Parameter(description = "Product ID") @PathVariable UUID id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully"));
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate product", description = "Activates a product")
    public ResponseEntity<ApiResponse<Void>> activateProduct(
            @Parameter(description = "Product ID") @PathVariable UUID id) {
        productService.activateProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product activated successfully"));
    }

    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate product", description = "Deactivates a product")
    public ResponseEntity<ApiResponse<Void>> deactivateProduct(
            @Parameter(description = "Product ID") @PathVariable UUID id) {
        productService.deactivateProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deactivated successfully"));
    }

    @PatchMapping("/{id}/stock")
    @Operation(summary = "Update stock", description = "Updates product stock quantity")
    public ResponseEntity<ApiResponse<Void>> updateStock(
            @Parameter(description = "Product ID") @PathVariable UUID id,
            @Parameter(description = "Quantity to add (negative to reduce)") @RequestParam int quantity) {
        productService.updateStock(id, quantity);
        return ResponseEntity.ok(ApiResponse.success("Stock updated successfully"));
    }
}

