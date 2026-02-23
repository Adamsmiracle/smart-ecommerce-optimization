package com.miracle.smart_ecommerce_jpa.domain.cart.service;

import com.miracle.smart_ecommerce_jpa.common.response.PageResponse;
import com.miracle.smart_ecommerce_jpa.domain.cart.dto.AddToCartRequest;
import com.miracle.smart_ecommerce_jpa.domain.cart.dto.CartResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service interface for Cart operations.
 */
public interface CartService {

    /**
     * Get all carts with pagination - admin use
     */
    PageResponse<CartResponse> getAllCarts(Pageable pageable);

    /**
     * Get cart by user ID (creates one if not exists)
     */
    CartResponse getCartByUserId(UUID userId);

    /**
     * Add item to cart
     */
    CartResponse addItemToCart(UUID userId, AddToCartRequest request);

    /**
     * Update item quantity
     */
    CartResponse updateItemQuantity(UUID userId, UUID itemId, int quantity);

    /**
     * Remove item from cart
     */
    CartResponse removeItemFromCart(UUID userId, UUID itemId);

    /**
     * Clear all items from cart
     */
    void clearCart(UUID userId);

    /**
     * Get total item count in cart
     */
    int getCartItemCount(UUID userId);
}