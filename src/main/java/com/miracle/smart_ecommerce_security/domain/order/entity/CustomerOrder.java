package com.miracle.smart_ecommerce_security.domain.order.entity;

import com.miracle.smart_ecommerce_security.domain.BaseModel;
import com.miracle.smart_ecommerce_security.domain.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Customer Order JPA entity - represents customer_order table.
 */
@Entity
@Table(name = "customer_order")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CustomerOrder extends BaseModel {

    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @NotBlank(message = "Order number is required")
    @Size(max = 50, message = "Order number cannot exceed 50 characters")
    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @Size(max = 30, message = "Status cannot exceed 30 characters")
    @Column(name = "status", length = 30, nullable = false)
    @Builder.Default
    private String status = OrderStatus.PENDING.name().toLowerCase();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id")
    @ToString.Exclude
    private PaymentMethod paymentMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipping_method_id")
    @ToString.Exclude
    private ShippingMethod shippingMethod;

    @NotNull(message = "Subtotal is required")
    @DecimalMin(value = "0.00", message = "Subtotal must be non-negative")
    @Column(name = "subtotal", nullable = false, precision = 19, scale = 2)
    private BigDecimal subtotal;

    @NotNull(message = "Total is required")
    @DecimalMin(value = "0.00", message = "Total must be non-negative")
    @Column(name = "total", nullable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @ToString.Exclude
    private List<OrderItem> orderItems = new ArrayList<>();

    // Payment status field - persisted as a string (e.g., pending, paid, failed)
    @Size(max = 30, message = "Payment status cannot exceed 30 characters")
    @Column(name = "payment_status", length = 30)
    @Builder.Default
    private String paymentStatus = PaymentStatus.PENDING.name().toLowerCase();

    /**
     * Generate unique order number using UUID to avoid collisions under high load
     */
    public static String generateOrderNumber() {
        String timestamp = java.time.format.DateTimeFormatter
                .ofPattern("yyyyMMdd")
                .format(OffsetDateTime.now());
        String uniquePart = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "ORD-" + timestamp + "-" + uniquePart;
    }

    /**
     * Add order item and keep both sides in sync
     */
    public void addOrderItem(OrderItem item) {
        if (item == null) {
            throw new IllegalArgumentException("Order item cannot be null");
        }
        if (orderItems == null) {
            orderItems = new ArrayList<>();
        }
        orderItems.add(item);
        item.setOrder(this);
    }

    /**
     * Calculate totals - shippingCost passed in to avoid lazy-loading shippingMethod outside a transaction
     */
    public void calculateTotals(BigDecimal shippingCost) {
        if (orderItems == null || orderItems.isEmpty()) {
            this.subtotal = BigDecimal.ZERO;
        } else {
            this.subtotal = orderItems.stream()
                    .map(OrderItem::getTotalPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        this.total = subtotal.add(shippingCost != null ? shippingCost : BigDecimal.ZERO);
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