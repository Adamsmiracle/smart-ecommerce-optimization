package com.miracle.smart_ecommerce_security.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Refresh token request — provide the refreshToken from your login or last refresh response")
public class RefreshRequest {

    @NotBlank(message = "Refresh token is required")
    @Schema(description = "The refresh token received at login or from a previous refresh call",
            example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;
}
