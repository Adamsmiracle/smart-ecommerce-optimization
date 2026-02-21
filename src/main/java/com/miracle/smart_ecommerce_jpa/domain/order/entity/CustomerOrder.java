package com.miracle.smart_ecommerce_jpa.domain.order.entity;

import com.miracle.smart_ecommerce_jpa.domain.BaseModel;
import com.miracle.smart_ecommerce_jpa.domain.user.entity.User;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Customer Order domain model (POJO) - represents customer_order table.
 * No JPA annotations - used with raw JDBC.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class CustomerOrder extends BaseModel {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "Order number is required")
    @Size(max = 50, message = "Order number cannot exceed 50 characters")
    private String orderNumber;

    @Size(max = 30, message = "Status cannot exceed 30 characters")
    @Builder.Default
    private String status = OrderStatus.PENDING.name().toLowerCase();

    private UUID paymentMethodId;

    @Size(max = 30, message = "Payment status cannot exceed 30 characters")
    @Builder.Default
    private String paymentStatus = PaymentStatus.PENDING.name().toLowerCase();

    private UUID shippingMethodId;

    @NotNull(message = "Subtotal is required")
    @DecimalMin(value = "0.00", message = "Subtotal must be non-negative")
    private BigDecimal subtotal;

    @NotNull(message = "Total is required")
    @DecimalMin(value = "0.00", message = "Total must be non-negative")
    private BigDecimal total;

    // Transient fields for relationships (populated when needed)
    private transient User user;
    private transient PaymentMethod paymentMethod;
    private transient ShippingMethod shippingMethod;

    @Builder.Default
    private transient List<OrderItem> orderItems = new ArrayList<>();

    /**
     * Generate unique order number
     */
    public static String generateOrderNumber() {
        String timestamp = java.time.format.DateTimeFormatter
                .ofPattern("yyyyMMdd")
                .format(OffsetDateTime.now());
        String randomPart = String.format("%06d", new Random().nextInt(999999));
        return "ORD-" + timestamp + "-" + randomPart;
    }

    /**
     * Add order item
     */
    public void addOrderItem(OrderItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Order item cannot be null");
        }
        if (orderItems == null) {
            orderItems = new ArrayList<>();
        }
        orderItems.add(item);
        item.setOrderId(this.getId());
    }

    /**
     * Calculate totals
     */
    public void calculateTotals() {
        if (orderItems == null || orderItems.isEmpty()) {
            this.subtotal = BigDecimal.ZERO;
        } else {
            this.subtotal = orderItems.stream()
                    .map(OrderItem::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        // Calculate total as subtotal + shipping cost
        BigDecimal shippingCost = BigDecimal.ZERO;
        if (shippingMethod != null && shippingMethod.getPrice() != null) {
            shippingCost = shippingMethod.getPrice();
        }
        
        this.total = subtotal.add(shippingCost);
    }

    /**
     * Get order item count
     */
    public int getItemCount() {
        if (orderItems == null) return 0;
        return orderItems.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();
    }

    /**
     * Check if order can be cancelled
     */
    public boolean canBeCancelled() {
        return OrderStatus.PENDING.name().equalsIgnoreCase(status) ||
               OrderStatus.CONFIRMED.name().equalsIgnoreCase(status) ||
               OrderStatus.PROCESSING.name().equalsIgnoreCase(status);
    }

    /**
     * Order status enum
     */
    public enum OrderStatus {
        PENDING,
        CONFIRMED,
        PROCESSING,
        SHIPPED,
        OUT_FOR_DELIVERY,
        DELIVERED,
        CANCELLED,
        REFUNDED,
        FAILED
    }

    /**
     * Payment status enum
     */
    public enum PaymentStatus {
        PENDING,
        PAID,
        FAILED,
        REFUNDED,
        PARTIALLY_REFUNDED
    }
}
