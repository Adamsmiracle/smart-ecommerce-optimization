package com.miracle.smart_ecommerce_security.domain.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory token blacklist for revoked JWTs.
 *
 * DSA concept: Uses a ConcurrentHashMap for O(1) lookup/insert of blacklisted token IDs (JTIs).
 * In production this would be backed by Redis with TTL matching JWT expiry;
 * this in-memory implementation is suitable for single-instance deployments.
 */
@Service
@Slf4j
public class TokenBlacklistService {

    /**
     * Map of JTI → expiry instant.  Tokens are stored until their natural expiry
     * to prevent replay attacks, then cleaned up by the scheduled purge.
     */
    private final ConcurrentHashMap<String, Instant> blacklist = new ConcurrentHashMap<>();

    /**
     * Blacklist a token by its JTI so it cannot be used again.
     *
     * @param jti    the unique token identifier
     * @param expiry when the token would naturally expire
     */
    public void blacklist(String jti, Instant expiry) {
        blacklist.put(jti, expiry);
        log.info("TOKEN_BLACKLISTED — JTI: {} — Expiry: {} — BlacklistSize: {} — CID: {}",
                jti, expiry, blacklist.size(), MDC.get("correlationId"));
    }

    /**
     * Check whether a token is blacklisted.  O(1) HashMap lookup.
     */
    public boolean isBlacklisted(String jti) {
        return blacklist.containsKey(jti);
    }

    /**
     * Periodic cleanup of expired entries to prevent unbounded memory growth.
     * Runs every 15 minutes.
     */
    @Scheduled(fixedRate = 900_000)
    public void purgeExpired() {
        int before = blacklist.size();
        Instant now = Instant.now();
        blacklist.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
        int removed = before - blacklist.size();
        if (removed > 0) {
            log.info("TOKEN_BLACKLIST_PURGE — Removed: {} — Remaining: {}", removed, blacklist.size());
        }
    }

    /**
     * Get current blacklist size (for monitoring/actuator).
     */
    public int size() {
        return blacklist.size();
    }
}

