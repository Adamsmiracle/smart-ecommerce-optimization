package com.miracle.smart_ecommerce_jpa.domain.auth.service;

import com.miracle.smart_ecommerce_jpa.domain.auth.dto.AuthResponse;

public interface AuthService {
    AuthResponse authenticate(String email, String password);
}

