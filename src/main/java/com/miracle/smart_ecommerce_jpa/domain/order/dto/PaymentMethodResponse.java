package com.miracle.smart_ecommerce_jpa.domain.order.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethodResponse {
    private UUID id;
    private UUID userId;
    private String paymentType;
    private String provider;
    private String maskedAccount;
    private OffsetDateTime expiryDate;
    private OffsetDateTime createdAt;
}

