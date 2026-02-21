package com.miracle.smart_ecommerce_jpa.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for creating a new address.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateAddressRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @Size(max = 500, message = "Address line cannot exceed 500 characters")
    private String addressLine;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City cannot exceed 100 characters")
    private String city;

    @Size(max = 100, message = "Region cannot exceed 100 characters")
    private String region;

    @NotBlank(message = "Country is required")
    @Size(max = 100, message = "Country cannot exceed 100 characters")
    private String country;

    @Size(max = 20, message = "Postal code cannot exceed 20 characters")
    private String postalCode;

    @Builder.Default
    private Boolean isDefault = false;

    @Pattern(regexp = "^(shipping|billing)$", message = "Address type must be 'shipping' or 'billing'")
    @Builder.Default
    private String addressType = "shipping";
}
