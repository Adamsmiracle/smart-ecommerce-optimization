package com.miracle.smart_ecommerce_security.domain.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * DTO for updating top-level editable order fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderRequest {

    private UUID paymentMethodId;

    private UUID shippingAddressId;

    private UUID shippingMethodId;

    @Size(max = 1000, message = "Customer notes cannot exceed 1000 characters")
    private String customerNotes;

    // Optional list of order item updates. If null, items are left unchanged.
    // Each item may provide an `id` for existing items or null for new items.
    private List<OrderItemUpdateRequest> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemUpdateRequest {
        // existing item id (optional) - if provided, we'll update/delete that item
        private UUID id;

        // product to add/update
        private UUID productId;

        @Min(value = 1, message = "Quantity must be at least 1")
        private Integer quantity;
    }
}
