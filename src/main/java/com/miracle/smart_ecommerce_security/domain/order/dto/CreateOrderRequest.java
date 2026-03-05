package com.miracle.smart_ecommerce_security.domain.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for creating a new order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    private UUID paymentMethodId;

    private UUID shippingAddressId;

    private UUID shippingMethodId;

    @Size(max = 1000, message = "Customer notes cannot exceed 1000 characters")
    private String customerNotes;

    @NotEmpty(message = "Order items are required")
    @Valid
    private List<OrderItemRequest> items;

    /**
     * Order item request
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest {

        @NotNull(message = "Product ID is required")
        private UUID productId;

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;
    }
}

