package com.miracle.smart_ecommerce_security.domain.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Service for logging token activities throughout the application.
 * 
 * <p>Note: This service provides logging only. JWT tokens are stateless by design -
 * the token itself contains all necessary information. We do NOT maintain an in-memory
 * store of "active tokens" as it would create memory pressure and provide no real benefit
 * since token validity is determined by cryptographic signature and expiry, not by server-side state.</p>
 * 
 * <p>This contrasts with token blacklisting (via TokenBlacklistService) which IS necessary
 * for handling logout and token revocation before natural expiry.</p>
 * 
 * <p>All logging methods are async to prevent blocking the request thread.</p>
 */
@Service
@Slf4j
public class TokenActivityService {

    /**
     * Logs a successful token validation event.
     * This is called on every authenticated request.
     */
    @Async
    public void logTokenValidation(String jti, String userId, String role, String clientIp, String userAgent) {
        log.debug("TOKEN_VALIDATION_SUCCESS — JTI: {} — UserId: {} — Role: {} — IP: {} — CID: {}", 
            jti, userId, role, clientIp, MDC.get("correlationId"));
    }
    
    /**
     * Logs a token generation event.
     */
    @Async
    public void logTokenGeneration(String jti, String userId, String role, String clientIp, String userAgent) {
        log.info("TOKEN_GENERATED — JTI: {} — UserId: {} — Role: {} — IP: {} — CID: {}",
            jti, userId, role, clientIp, MDC.get("correlationId"));
    }
    
    /**
     * Logs a failed token validation attempt.
     */
    @Async
    public void logTokenValidationFailure(String jti, String reason, String clientIp, String userAgent) {
        log.warn("TOKEN_VALIDATION_FAILED — JTI: {} — Reason: {} — IP: {} — UserAgent: {} — CID: {}", 
            jti != null ? jti : "unknown", reason, clientIp, userAgent, MDC.get("correlationId"));
    }
    
    /**
     * Logs token revocation (logout or token rotation).
     */
    @Async
    public void logTokenRevocation(String jti, String userId, String reason, String clientIp) {
        log.info("TOKEN_REVOKED — JTI: {} — UserId: {} — Reason: {} — IP: {} — CID: {}", 
            jti, userId, reason, clientIp, MDC.get("correlationId"));
    }
}
