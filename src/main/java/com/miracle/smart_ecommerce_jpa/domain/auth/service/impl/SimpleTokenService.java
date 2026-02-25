package com.miracle.smart_ecommerce_jpa.domain.auth.service.impl;

import com.miracle.smart_ecommerce_jpa.domain.auth.service.TokenActivityService; // NEW IMPORT
import com.miracle.smart_ecommerce_jpa.domain.auth.service.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

@Service
@Primary
public class SimpleTokenService implements TokenService {

    private static final Logger log = LoggerFactory.getLogger(SimpleTokenService.class);
    private final TokenActivityService tokenActivityService; // NEW FIELD

    public SimpleTokenService(TokenActivityService tokenActivityService) {
        this.tokenActivityService = tokenActivityService;
    }

    /**
     * Generate a simple token of format: userId:role:uuid
     * where uuid = name-based UUID from bytes of userId:role
     */
    @Override
    public String generateToken(UUID userId, String role) {
        String payload = userId.toString() + ":" + role;
        UUID derived = UUID.nameUUIDFromBytes(payload.getBytes(StandardCharsets.UTF_8));
        String token = userId.toString() + ":" + role + ":" + derived.toString();
        
        log.info("TOKEN_GENERATED - UserId: {} - Role: {} - CID: {}", 
            userId, role, MDC.get("correlationId"));
        log.debug("TOKEN_GENERATED_DEBUG - Token: {} - CID: {}", token, MDC.get("correlationId"));
        
        // Log token activity (will be enhanced when called from AuthController with request context)
        if (tokenActivityService != null) {
            tokenActivityService.logTokenGeneration(token, userId.toString(), role, "unknown", "unknown");
        }
        
        return token;
    }

    /**
     * Validate token: parse parts and verify derived UUID matches.
     */
    @Override
    public Optional<TokenService.AuthPrincipal> validateToken(String token) {
        if (token == null || token.isBlank()) {
            log.warn("TOKEN_VALIDATION_FAILED - Token is null or empty - CID: {}", MDC.get("correlationId"));
            return Optional.empty();
        }
        
        try {
            log.debug("TOKEN_VALIDATION_START - Token: {} - CID: {}", token, MDC.get("correlationId"));
            
            String[] parts = token.split(":");
            if (parts.length < 3) {
                log.warn("TOKEN_VALIDATION_FAILED - Invalid token format - Expected 3 parts, got {} - CID: {}", 
                    parts.length, MDC.get("correlationId"));
                return Optional.empty();
            }
            
            String userIdStr = parts[0];
            String role = parts[1];
            String uuidStr = parts[2];
            
            try {
                UUID userId = UUID.fromString(userIdStr);
                String payload = userIdStr + ":" + role;
                UUID expected = UUID.nameUUIDFromBytes(payload.getBytes(StandardCharsets.UTF_8));
                
                if (!expected.toString().equals(uuidStr)) {
                    log.warn("TOKEN_VALIDATION_FAILED - Token signature mismatch - UserId: {} - Role: {} - CID: {}", 
                        userIdStr, role, MDC.get("correlationId"));
                    return Optional.empty();
                }
                
                log.info("TOKEN_VALIDATION_SUCCESS - UserId: {} - Role: {} - CID: {}", 
                    userId, role, MDC.get("correlationId"));
                
                return Optional.of(new TokenService.AuthPrincipal(userId, role));
                
            } catch (IllegalArgumentException e) {
                log.warn("TOKEN_VALIDATION_FAILED - Invalid UUID format in token - UserIdStr: {} - CID: {}", 
                    userIdStr, MDC.get("correlationId"));
                return Optional.empty();
            }
            
        } catch (Exception ex) {
            log.error("TOKEN_VALIDATION_ERROR - Unexpected error during token validation - CID: {}", 
                MDC.get("correlationId"), ex);
            return Optional.empty();
        }
    }
}












