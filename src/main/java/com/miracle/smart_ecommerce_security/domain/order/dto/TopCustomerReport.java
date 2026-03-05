package com.miracle.smart_ecommerce_security.domain.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO representing a single row from the top-customers-by-spending native SQL report.
 * Returned by {@code OrderRepository.findTopCustomersBySpending}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopCustomerReport {

    /** The customer's user ID. */
    private UUID userId;

    /** Customer's first name. */
    private String firstName;

    /** Customer's last name. */
    private String lastName;

    /** Customer's email address. */
    private String emailAddress;

    /** Number of completed/delivered orders. */
    private Long totalOrders;

    /** Sum of all completed/delivered order totals. */
    private BigDecimal totalSpent;

    /**
     * Maps a raw Object[] row returned by the native SQL query to a {@link TopCustomerReport}.
     * Column order must match the SELECT clause in {@code OrderRepository.findTopCustomersBySpending}:
     * [0] user_id, [1] first_name, [2] last_name, [3] email_address, [4] total_orders, [5] total_spent
     */
    public static TopCustomerReport fromRow(Object[] row) {
        return TopCustomerReport.builder()
                .userId(row[0] != null ? UUID.fromString(row[0].toString()) : null)
                .firstName(row[1] != null ? row[1].toString() : null)
                .lastName(row[2] != null ? row[2].toString() : null)
                .emailAddress(row[3] != null ? row[3].toString() : null)
                .totalOrders(row[4] != null ? ((Number) row[4]).longValue() : 0L)
                .totalSpent(row[5] != null ? new BigDecimal(row[5].toString()) : BigDecimal.ZERO)
                .build();
    }
}

