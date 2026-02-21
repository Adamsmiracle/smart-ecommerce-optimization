package com.miracle.smart_ecommerce_jpa.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Data;



@Data
public class AuthRequest {
    @Email
    @NotNull(message = "email is required")
    private String email;

    @NotNull(message = "password is required")
    private String password;
}

