package com.miracle.smart_ecommerce_jpa.domain.cart.entity;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Cart Item domain model (POJO) - represents cart_item table.
 * No JPA annotations - used with raw JDBC.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class CartItem {

    @NotNull(message = "id is required")
    private UUID id;

    @NotNull(message = "Cart ID is required")
    private UUID cartId;

    @NotNull(message = "Product ID is required")
    private UUID productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    @Builder.Default
    private Integer quantity = 1;

    /**
     * Helper to compute subtotal given a unit price; product lookup happens in service
     */
    public java.math.BigDecimal subtotal(java.math.BigDecimal unitPrice) {
        if (unitPrice == null) return java.math.BigDecimal.ZERO;
        return unitPrice.multiply(java.math.BigDecimal.valueOf(this.quantity));
    }
}
