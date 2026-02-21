package com.miracle.smart_ecommerce_jpa.domain.order.entity;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Payment Method domain model (POJO) - represents payment_method table.
 * No JPA annotations - used with raw JDBC.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class PaymentMethod {

    @NotNull(message = "is is required")
    private UUID id;

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "Payment type is required")
    @Size(max = 50, message = "Payment type cannot exceed 50 characters")
    private String paymentType;

    @Size(max = 100, message = "Provider cannot exceed 100 characters")
    private String provider;

    @NotBlank(message = "Account number is required")
    private String accountNumber;

    @NotNull(message = "Expiry date is required")

    private OffsetDateTime expiryDate;

    @NotNull(message = "createAt is required")
    private OffsetDateTime createdAt;

    /**
     * Get masked account number (show last 4 digits only)
     */
    public String getMaskedAccountNumber() {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "****";
        }
        return "**** **** **** " + accountNumber.substring(accountNumber.length() - 4);
    }

    /**
     * Get display name (e.g., "Visa ending in 4242")
     */
    public String getDisplayName() {
        String providerName = provider != null ? provider : paymentType;
        String lastFour = accountNumber != null && accountNumber.length() >= 4
                ? accountNumber.substring(accountNumber.length() - 4)
                : "****";
        return providerName + " ending in " + lastFour;
    }
}

