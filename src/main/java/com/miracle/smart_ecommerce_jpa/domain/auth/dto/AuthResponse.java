package com.miracle.smart_ecommerce_jpa.domain.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class AuthResponse {
    private UUID userId;
    private String role;
}
