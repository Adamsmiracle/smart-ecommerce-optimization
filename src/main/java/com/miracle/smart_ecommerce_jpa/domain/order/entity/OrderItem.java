package com.miracle.smart_ecommerce_jpa.domain.order.entity;

import com.miracle.smart_ecommerce_jpa.domain.product.entity.Product;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Order Item domain model (POJO) - represents order_item table.
 * No JPA annotations - used with raw JDBC.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class OrderItem {

    @NotNull(message = "id is required")
    private UUID id;

    @NotNull(message = "Order ID is required")
    private UUID orderId;

    @NotNull(message = "Product ID is required")
    private UUID productId;

    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.00", message = "Unit price must be non-negative")
    private BigDecimal unitPrice;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    // Derived total price (not stored in DB). Use getter to compute when needed.
    public BigDecimal getTotalPrice() {
        if (unitPrice == null || quantity == null) return BigDecimal.ZERO;
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }


    /**
     * Create order item from product
     */
    public static OrderItem fromProduct(Product product, int quantity) {
        if (product == null) {
            throw new IllegalArgumentException("Product cannot be null");
        }
        if (quantity < 1) {
            throw new IllegalArgumentException("Quantity must be at least 1");
        }

        BigDecimal unitPrice = product.getPrice();

        return OrderItem.builder()
                .productId(product.getId())
                .unitPrice(unitPrice)
                .quantity(quantity)
                .build();
    }
}
