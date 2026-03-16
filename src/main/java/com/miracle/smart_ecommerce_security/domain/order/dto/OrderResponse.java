package com.miracle.smart_ecommerce_security.domain.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for Order.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

    private UUID id;
    private UUID userId;
    private String customerName;
    private String orderNumber;
    private String status;
    private String paymentStatus;
    private UUID paymentMethodId;
    private UUID shippingMethodId;
    private BigDecimal subtotal;
    private BigDecimal shippingCost;
    private BigDecimal total;
    private Integer itemCount;
    private String customerNotes;
    private OffsetDateTime createdAt;
    private OffsetDateTime cancelledAt;
    private List<OrderItemResponse> items;
    private ShippingAddressResponse shippingAddress;
    private ShippingMethodResponse shippingMethod;

    /**
     * Order item response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        private UUID id;
        private UUID productId;
        private String productName;
        private BigDecimal unitPrice;
        private Integer quantity;
        private BigDecimal totalPrice;
    }

    /**
     * Shipping address response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingAddressResponse {
        private UUID id;
        private String addressLine;
        private String city;
        private String region;
        private String country;
        private String postalCode;
        private String fullAddress;
    }

    /**
     * Shipping method response
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ShippingMethodResponse {
        private UUID id;
        private String name;
        private BigDecimal price;
        private String estimatedDelivery;
    }
}
