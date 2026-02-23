package com.miracle.smart_ecommerce_jpa.domain.user.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for Address.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {

    private UUID id;
    private UUID userId;
    private String addressLine;
    private String city;
    private String region;
    private String country;
    private String postalCode;
    private String addressType;
    private Boolean isDefault;
    private OffsetDateTime createdAt;

    /**
     * Get formatted full address
     */
    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (addressLine != null && !addressLine.isEmpty()) {
            sb.append(addressLine).append(", ");
        }
        sb.append(city);
        if (region != null && !region.isEmpty()) {
            sb.append(", ").append(region);
        }
        if (postalCode != null && !postalCode.isEmpty()) {
            sb.append(" ").append(postalCode);
        }
        sb.append(", ").append(country);
        return sb.toString();
    }
}