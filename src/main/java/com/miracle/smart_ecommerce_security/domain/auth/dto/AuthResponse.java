package com.miracle.smart_ecommerce_security.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@Schema(description = "Authentication response containing the signed JWT and its metadata")
public class AuthResponse {

    @Schema(description = "Authenticated user's internal ID", example = "1c6627ae-6bff-4916-ae2d-82be59629e2c")
    private UUID userId;

    @Schema(description = "User's role", example = "CUSTOMER", allowableValues = {"ADMIN", "STAFF", "CUSTOMER"})
    private String role;

    @Schema(description = "Signed JWT — include as: Authorization: Bearer <token>")
    private String token;

    @Schema(description = "Signed JWT — include as: Authorization: Bearer <refreshToken>")
    private String refreshToken;

    @Schema(description = "Token type — always Bearer", example = "Bearer")
    @JsonProperty("tokenType")
    @Builder.Default
    private String tokenType = "Bearer";

    @Schema(description = "How long the access token is valid for, e.g. '24 hours' or '1 day'")
    @JsonProperty("expiresIn")
    private String expiresIn;

    @Schema(description = "UTC timestamp when the token was issued")
    @JsonProperty("issuedAt")
    private Instant issuedAt;

    @Schema(description = "UTC timestamp when the access token expires")
    @JsonProperty("expiresAt")
    private Instant expiresAt;

    @Schema(description = "How long the refresh token is valid for, e.g. '7 days' or '12 hours'")
    @JsonProperty("refreshExpiresIn")
    private String refreshExpiresIn;
}

