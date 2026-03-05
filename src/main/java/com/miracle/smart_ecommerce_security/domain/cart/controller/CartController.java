package com.miracle.smart_ecommerce_security.domain.cart.controller;

import com.miracle.smart_ecommerce_security.common.response.ApiResponse;
import com.miracle.smart_ecommerce_security.domain.cart.dto.AddToCartRequest;
import com.miracle.smart_ecommerce_security.domain.cart.dto.CartResponse;
import com.miracle.smart_ecommerce_security.domain.cart.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST Controller for Shopping Cart management.
 */
@RestController
@RequestMapping("/api/cart")
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'CUSTOMER')")
@Tag(name = "Cart", description = "Shopping cart management APIs")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    @Operation(summary = "Get all carts", description = "Retrieves all shopping carts with pagination (Admin)")
    public ResponseEntity<ApiResponse<Page<CartResponse>>> getAllCarts(
            @PageableDefault(size = 10) Pageable pageable) {
        Page<CartResponse> carts = cartService.getAllCarts(pageable);
        return ResponseEntity.ok(ApiResponse.success(carts));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get cart", description = "Retrieves user's shopping cart")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        CartResponse cart = cartService.getCartByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(cart));
    }

    @PostMapping("/user/{userId}/items")
    @Operation(summary = "Add item to cart", description = "Adds a product to the shopping cart")
    public ResponseEntity<ApiResponse<CartResponse>> addItemToCart(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @Valid @RequestBody AddToCartRequest request) {
        CartResponse cart = cartService.addItemToCart(userId, request);
        return ResponseEntity.ok(ApiResponse.success(cart, "Item added to cart"));
    }

    @PutMapping("/user/{userId}/items/{itemId}")
    @Operation(summary = "Update item quantity", description = "Updates the quantity of a cart item")
    public ResponseEntity<ApiResponse<CartResponse>> updateItemQuantity(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @Parameter(description = "Cart item ID") @PathVariable UUID itemId,
            @Parameter(description = "New quantity") @RequestParam int quantity) {
        CartResponse cart = cartService.updateItemQuantity(userId, itemId, quantity);
        return ResponseEntity.ok(ApiResponse.success(cart, "Cart item updated"));
    }

    @DeleteMapping("/user/{userId}/items/{itemId}")
    @Operation(summary = "Remove item from cart", description = "Removes a product from the shopping cart")
    public ResponseEntity<ApiResponse<CartResponse>> removeItemFromCart(
            @Parameter(description = "User ID") @PathVariable UUID userId,
            @Parameter(description = "Cart item ID") @PathVariable UUID itemId) {
        CartResponse cart = cartService.removeItemFromCart(userId, itemId);
        return ResponseEntity.ok(ApiResponse.success(cart, "Item removed from cart"));
    }

    @DeleteMapping("/user/{userId}")
    @Operation(summary = "Clear cart", description = "Removes all items from the shopping cart")
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiResponse.success("Cart cleared successfully"));
    }

    @GetMapping("/user/{userId}/count")
    @Operation(summary = "Get cart item count", description = "Returns the total number of items in cart")
    public ResponseEntity<ApiResponse<Integer>> getCartItemCount(
            @Parameter(description = "User ID") @PathVariable UUID userId) {
        int count = cartService.getCartItemCount(userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/count")
    @Operation(summary = "Get cart item count (query)", description = "Returns the total number of items in cart — accepts userId as query parameter")
    public ResponseEntity<ApiResponse<Integer>> getCartItemCountByQuery(
            @Parameter(description = "User ID (query param)") @RequestParam UUID userId) {
        int count = cartService.getCartItemCount(userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }
}
