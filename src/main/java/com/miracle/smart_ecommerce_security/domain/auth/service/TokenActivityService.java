package com.miracle.smart_ecommerce_security.domain.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Service for tracking and logging token activities throughout the application.
 * Provides visibility into token usage patterns and potential security issues.
 */
@Service
public class TokenActivityService {

    private static final Logger log = LoggerFactory.getLogger(TokenActivityService.class);
    
    // In-memory store for tracking active tokens (in production, this could be Redis/database)
    private final ConcurrentMap<String, TokenActivity> activeTokens = new ConcurrentHashMap<>();
    
    /**
     * Records a successful token validation event
     */
    public void logTokenValidation(String token, String userId, String role, String clientIp, String userAgent) {
        String tokenId = extractTokenId(token);
        
        // Update or create token activity record
        TokenActivity activity = activeTokens.compute(tokenId, (key, existing) -> {
            if (existing == null) {
                return new TokenActivity(tokenId, userId, role, clientIp, userAgent);
            } else {
                existing.incrementValidationCount();
                existing.setLastUsed(Instant.now());
                return existing;
            }
        });
        
        log.info("TOKEN_ACTIVITY_VALIDATION - TokenId: {} - UserId: {} - Role: {} - IP: {} - UserAgent: {} - ValidationCount: {} - CID: {}", 
            tokenId, userId, role, clientIp, userAgent, activity.getValidationCount(), MDC.get("correlationId"));
    }
    
    /**
     * Records a token generation event
     */
    public void logTokenGeneration(String jti, String userId, String role, String clientIp, String userAgent) {
        TokenActivity activity = new TokenActivity(jti, userId, role, clientIp, userAgent);
        activeTokens.put(jti, activity);

        log.info("TOKEN_ACTIVITY_GENERATION - TokenId: {} - UserId: {} - Role: {} - IP: {} - UserAgent: {} - CID: {}",
            jti, userId, role, clientIp, userAgent, MDC.get("correlationId"));
    }
    
    /**
     * Records a failed token validation attempt
     */
    public void logTokenValidationFailure(String token, String reason, String clientIp, String userAgent) {
        String tokenId = extractTokenId(token);
        
        log.warn("TOKEN_ACTIVITY_FAILURE - TokenId: {} - Reason: {} - IP: {} - UserAgent: {} - CID: {}", 
            tokenId, reason, clientIp, userAgent, MDC.get("correlationId"));
    }
    
    /**
     * Records token revocation/expiry
     */
    public void logTokenRevocation(String token, String reason, String clientIp) {
        String tokenId = extractTokenId(token);
        TokenActivity activity = activeTokens.remove(tokenId);
        
        if (activity != null) {
            log.info("TOKEN_ACTIVITY_REVOCATION - TokenId: {} - UserId: {} - Role: {} - Reason: {} - IP: {} - TotalValidations: {} - CID: {}", 
                tokenId, activity.getUserId(), activity.getRole(), reason, clientIp, 
                activity.getValidationCount(), MDC.get("correlationId"));
        }
    }
    
    /**
     * Gets statistics about token usage
     */
    public TokenUsageStats getTokenUsageStats() {
        int totalTokens = activeTokens.size();
        long totalValidations = activeTokens.values().stream()
                .mapToLong(TokenActivity::getValidationCount)
                .sum();
        
        return new TokenUsageStats(totalTokens, totalValidations);
    }
    
    /**
     * Extracts a short token ID from a JWT string.
     * JWTs have the format: header.payload.signature (3 parts separated by ".")
     * We use the last 8 chars of the signature as a short identifier for logging.
     */
    private String extractTokenId(String token) {
        if (token == null || token.isBlank()) return "unknown";
        String[] parts = token.split("\\.");
        if (parts.length == 3) {
            String sig = parts[2];
            return sig.length() >= 8 ? sig.substring(sig.length() - 8) : sig;
        }
        return "invalid";
    }
    
    /**
     * Inner class to track token activity
     */
    public static class TokenActivity {
        private final String tokenId;
        private final String userId;
        private final String role;
        private final String clientIp;
        private final String userAgent;
        private final Instant createdAt;
        private Instant lastUsed;
        private int validationCount;
        
        public TokenActivity(String tokenId, String userId, String role, String clientIp, String userAgent) {
            this.tokenId = tokenId;
            this.userId = userId;
            this.role = role;
            this.clientIp = clientIp;
            this.userAgent = userAgent;
            this.createdAt = Instant.now();
            this.lastUsed = Instant.now();
            this.validationCount = 1;
        }
        
        public void incrementValidationCount() {
            this.validationCount++;
            this.lastUsed = Instant.now();
        }
        
        public void setLastUsed(Instant lastUsed) {
            this.lastUsed = lastUsed;
        }
        
        // Getters
        public String getTokenId() { return tokenId; }
        public String getUserId() { return userId; }
        public String getRole() { return role; }
        public int getValidationCount() { return validationCount; }
        public Instant getCreatedAt() { return createdAt; }
        public Instant getLastUsed() { return lastUsed; }
    }
    
    /**
     * Statistics about token usage
     */
    public static class TokenUsageStats {
        private final int totalActiveTokens;
        private final long totalValidations;
        
        public TokenUsageStats(int totalActiveTokens, long totalValidations) {
            this.totalActiveTokens = totalActiveTokens;
            this.totalValidations = totalValidations;
        }
        
        public int getTotalActiveTokens() { return totalActiveTokens; }
        public long getTotalValidations() { return totalValidations; }
    }
}
