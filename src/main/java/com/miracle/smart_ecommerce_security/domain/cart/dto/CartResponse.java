package com.miracle.smart_ecommerce_security.domain.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for Shopping Cart.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {

    private UUID id;
    private UUID userId;
    private Integer totalItems;
    private BigDecimal totalValue;
    private OffsetDateTime createdAt;
    private List<CartItemResponse> items;

    /**
     * Cart item response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CartItemResponse {
        private UUID id;
        private UUID productId;
        private String productName;
        private String productImage;
        private BigDecimal unitPrice;
        private Integer quantity;
        private BigDecimal subtotal;
        private Boolean inStock;
        private Integer availableStock;
        private OffsetDateTime addedAt;
    }
}

