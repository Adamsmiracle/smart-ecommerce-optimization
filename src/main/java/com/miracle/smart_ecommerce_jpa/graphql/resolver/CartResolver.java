package com.miracle.smart_ecommerce_jpa.graphql.resolver;

import com.miracle.smart_ecommerce_jpa.domain.cart.dto.AddToCartRequest;
import com.miracle.smart_ecommerce_jpa.domain.cart.dto.CartResponse;
import com.miracle.smart_ecommerce_jpa.domain.cart.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.Map;
import java.util.UUID;

/**
 * GraphQL Resolver for Cart entity.
 * Handles all cart-related queries and mutations.
 */
@Controller
@RequiredArgsConstructor
public class CartResolver {

    private final CartService cartService;

    // ========================================================================
    // CART QUERIES
    // ========================================================================

    @QueryMapping
    public CartResponse cart(@Argument UUID userId) {
        return cartService.getCartByUserId(userId);
    }

    @QueryMapping
    public int cartItemCount(@Argument UUID userId) {
        return cartService.getCartItemCount(userId);
    }

    // ========================================================================
    // CART MUTATIONS
    // ========================================================================

    @MutationMapping
    public CartResponse addToCart(@Argument UUID userId, @Argument Map<String, Object> input) {
        AddToCartRequest request = AddToCartRequest.builder()
                .productId(UUID.fromString((String) input.get("productId")))
                .quantity((Integer) input.get("quantity"))
                .build();
        return cartService.addItemToCart(userId, request);
    }

    @MutationMapping
    public CartResponse updateCartItemQuantity(@Argument UUID userId,
                                               @Argument UUID itemId,
                                               @Argument int quantity) {
        return cartService.updateItemQuantity(userId, itemId, quantity);
    }

    @MutationMapping
    public CartResponse removeFromCart(@Argument UUID userId, @Argument UUID itemId) {
        return cartService.removeItemFromCart(userId, itemId);
    }

    @MutationMapping
    public boolean clearCart(@Argument UUID userId) {
        cartService.clearCart(userId);
        return true;
    }
}

