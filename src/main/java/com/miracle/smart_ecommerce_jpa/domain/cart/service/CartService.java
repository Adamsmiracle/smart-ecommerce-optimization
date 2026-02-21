package com.miracle.smart_ecommerce_jpa.domain.cart.service;

import com.miracle.smart_ecommerce_jpa.common.response.PageResponse;
import com.miracle.smart_ecommerce_jpa.domain.cart.dto.AddToCartRequest;
import com.miracle.smart_ecommerce_jpa.domain.cart.dto.CartResponse;

import java.util.UUID;

/**
 * Service interface for Cart operations.
 */
public interface CartService {

    /**
     * Get all carts with pagination
     */
    PageResponse<CartResponse> getAllCarts(int page, int size);

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
     * Clear cart
     */
    void clearCart(UUID userId);

    /**
     * Get cart item count
     */
    int getCartItemCount(UUID userId);
}

