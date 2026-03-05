package com.miracle.smart_ecommerce_security.domain.order.dto;

import lombok.*;

import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethodRequest {
    @NotNull
    private UUID userId;

    @NotBlank
    @Size(max = 50)
    private String paymentType;

    @Size(max = 100)
    private String provider;

    @NotBlank
    private String accountNumber;

    @NotNull
    private OffsetDateTime expiryDate;
}
