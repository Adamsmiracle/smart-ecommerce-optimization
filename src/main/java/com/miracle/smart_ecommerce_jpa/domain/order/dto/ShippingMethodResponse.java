package com.miracle.smart_ecommerce_jpa.domain.order.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingMethodResponse {
    private UUID id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer estimatedDays;
    private Boolean isActive = true;
    private OffsetDateTime createdAt;
}

