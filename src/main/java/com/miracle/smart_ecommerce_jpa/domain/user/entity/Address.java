package com.miracle.smart_ecommerce_jpa.domain.user.entity;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.stereotype.Component;

/**
 * Address domain model (POJO) - represents address table.
 * No JPA annotations - used with raw JDBC.
 */
@Component
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class Address {

    @NotNull(message = "id is required")
    private UUID id;

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "Address line is required")
    @Size(max = 255, message = "Address line cannot exceed 255 characters")
    private String addressLine;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String city;

    @NotBlank(message = "Region is required")
    @Size(max = 100, message = "Region cannot exceed 100 characters")
    private String region;

    @NotBlank(message = "Country is required")
    @Size(max = 100, message = "Country cannot exceed 100 characters")
    private String country;

    @Size(max = 20, message = "Postal code cannot exceed 20 characters")
    private String postalCode;


    @Size(max = 20, message = "Address type cannot exceed 20 characters")
    private String addressType; // 'shipping', 'billing', or NULL for both


    @NotNull(message = "createdAt is required")
    private OffsetDateTime createdAt;

    /**
     * Get formatted full address
     */
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        sb.append(addressLine);
        if (city != null) sb.append(", ").append(city);
        if (region != null) sb.append(", ").append(region);
        if (postalCode != null) sb.append(" ").append(postalCode);
        if (country != null) sb.append(", ").append(country);
        return sb.toString();
    }
}
