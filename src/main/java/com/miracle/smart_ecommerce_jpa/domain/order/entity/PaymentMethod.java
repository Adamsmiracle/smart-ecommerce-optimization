package com.miracle.smart_ecommerce_jpa.domain.order.entity;

import com.miracle.smart_ecommerce_jpa.domain.BaseModel;
import com.miracle.smart_ecommerce_jpa.domain.user.entity.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;

/**
 * Payment Method JPA entity - represents payment_method table.
 */
@Entity
@Table(name = "payment_method")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class PaymentMethod extends BaseModel {

    @NotNull(message = "User is required")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @NotBlank(message = "Payment type is required")
    @Size(max = 50, message = "Payment type cannot exceed 50 characters")
    @Column(name = "payment_type", nullable = false, length = 50)
    private String paymentType;

    @Size(max = 100, message = "Provider cannot exceed 100 characters")
    @Column(name = "provider", length = 100)
    private String provider;

    @NotBlank(message = "Account number is required")
    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @NotNull(message = "Expiry date is required")
    @Column(name = "expiry_date", nullable = false)
    private OffsetDateTime expiryDate;

    @Builder.Default
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

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