package com.miracle.smart_ecommerce_security.domain.auth.service.impl;

import com.miracle.smart_ecommerce_security.domain.auth.dto.AuthPrincipal;
import com.miracle.smart_ecommerce_security.domain.auth.dto.AuthResponse;
import com.miracle.smart_ecommerce_security.domain.auth.service.TokenBlacklistService;
import com.miracle.smart_ecommerce_security.domain.auth.service.TokenActivityService;
import com.miracle.smart_ecommerce_security.domain.auth.service.TokenService;
import io.jsonwebtoken.*;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/**
 * JWT-based token service using HMAC-SHA256 (HS256) for signing.
 *
 * <p>Token claims:</p>
 * <ul>
 *   <li>{@code sub}  — userId (UUID string)</li>
 *   <li>{@code role} — user role (ADMIN, CUSTOMER)</li>
 *   <li>{@code type} — "access" or "refresh"</li>
 *   <li>{@code iat}  — issued-at timestamp</li>
 *   <li>{@code exp}  — expiration timestamp</li>
 *   <li>{@code jti}  — unique token ID for blacklisting</li>
 * </ul>
 *
 * <p><b>DSA concepts applied:</b></p>
 * <ul>
 *   <li>HMAC-SHA256 hashing for tamper-proof digital signatures</li>
 *   <li>Token validation via cryptographic signature verification</li>
 *   <li>Blacklist lookup via ConcurrentHashMap O(1)</li>
 * </ul>
 */
@Service
@Slf4j
public class JwtTokenService implements TokenService {

    private final SecretKey signingKey;
    private final long expirationMs;
    private final long refreshExpirationMs;
    private final TokenBlacklistService blacklistService;
    private final TokenActivityService tokenActivityService;
    private final CacheManager cacheManager;

    public JwtTokenService(
            @Value("${jwt.secret}") String base64Secret,
            @Value("${jwt.expiration-ms}") long expirationMs,
            @Value("${jwt.refresh-expiration-ms:#{${jwt.expiration-ms} * 7}}") long refreshExpirationMs,
            TokenBlacklistService blacklistService,
            TokenActivityService tokenActivityService,
            CacheManager cacheManager) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
        this.blacklistService = blacklistService;
        this.tokenActivityService = tokenActivityService;
        this.cacheManager = cacheManager;
        log.info("JwtTokenService initialised — algorithm: HS256, accessExpiry: {}ms, refreshExpiry: {}ms",
                expirationMs, refreshExpirationMs);
    }

    // ── TokenService interface (backward-compatible) ──────────────────────

    @Override
    public String generateToken(UUID userId, String role) {
        return generateTokenPair(userId, role, "unknown", "unknown").accessToken();
    }

    public String generateToken(UUID userId, String role, String clientIp, String userAgent) {
        return generateTokenPair(userId, role, clientIp, userAgent).accessToken();
    }

    // ── Core: generate an access + refresh pair ───────────────────────────

    /**
     * Generates an access token and a refresh token in one call.
     * Each gets its own unique JTI so they can be blacklisted independently.
     *
     * Access token  — short-lived ({@code jwt.expiration-ms}, default 24 h)
     * Refresh token — long-lived  ({@code jwt.refresh-expiration-ms}, default 7× access)
     */
    public TokenPair generateTokenPair(UUID userId, String role, String clientIp, String userAgent) {
        Date now = new Date();

        // ── Access token ──────────────────────────────────────────────────
        String accessJti    = UUID.randomUUID().toString();
        Date   accessExpiry = new Date(now.getTime() + expirationMs);

        String accessToken = Jwts.builder()
                .id(accessJti)
                .subject(userId.toString())
                .claim("role", role)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(accessExpiry)
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();

        // ── Refresh token ─────────────────────────────────────────────────
        String refreshJti    = UUID.randomUUID().toString();
        Date   refreshExpiry = new Date(now.getTime() + refreshExpirationMs);

        String refreshToken = Jwts.builder()
                .id(refreshJti)
                .subject(userId.toString())
                .claim("role", role)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(refreshExpiry)
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();

        log.info("JWT_PAIR_GENERATED — UserId: {} — Role: {} — AccessJTI: {} — RefreshJTI: {} — CID: {}",
                userId, role, accessJti, refreshJti, MDC.get("correlationId"));

        if (tokenActivityService != null) {
            tokenActivityService.logTokenGeneration(accessJti, userId.toString(), role, clientIp, userAgent);
        }

        return new TokenPair(
                accessToken, refreshToken,
                now.toInstant(), accessExpiry.toInstant(), refreshExpiry.toInstant()
        );
    }

    /**
     * Builds a complete {@link AuthResponse} from a userId and role.
     * This is the single point of AuthResponse construction — controllers
     * call this instead of building the response themselves.
     */
    public AuthResponse buildAuthResponse(UUID userId, String role, String clientIp, String userAgent) {
        TokenPair pair = generateTokenPair(userId, role, clientIp, userAgent);
        return AuthResponse.builder()
                .userId(userId)
                .role(role)
                .token(pair.accessToken())
                .refreshToken(pair.refreshToken())
                .tokenType("Bearer")
                .expiresIn(humanReadableDuration(expirationMs))
                .issuedAt(pair.issuedAt())
                .expiresAt(pair.accessExpiresAt())
                .refreshExpiresIn(humanReadableDuration(refreshExpirationMs))
                .build();
    }

    /**
     * Converts a millisecond duration to a human-readable offset string.
     * Shows days when the duration is a whole number of days, otherwise hours.
     * Examples: 604800000ms → "7 days", 43200000ms → "12 hours"
     */
    private static String humanReadableDuration(long ms) {
        long hours = ms / 3_600_000;
        long days  = hours / 24;
        if (days > 0 && hours % 24 == 0) {
            return days + (days == 1 ? " day" : " days");
        }
        return hours + (hours == 1 ? " hour" : " hours");
    }

    /**
     * Validates a refresh token and — if valid — issues a new access + refresh pair.
     * The old refresh token is blacklisted immediately (token rotation prevents reuse).
     */
    public AuthResponse refreshAuthResponse(String refreshToken, String clientIp, String userAgent) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Refresh token must not be empty");
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(refreshToken)
                    .getPayload();

            String type = claims.get("type", String.class);
            if (!"refresh".equals(type)) {
                throw new IllegalArgumentException("Provided token is not a refresh token");
            }

            String jti    = claims.getId();
            String userId = claims.getSubject();
            String role   = claims.get("role", String.class);

            if (jti != null && blacklistService.isBlacklisted(jti)) {
                throw new IllegalArgumentException("Refresh token has been revoked");
            }

            // Token rotation — blacklist the used refresh token immediately
            if (jti != null) {
                blacklistService.blacklist(jti, claims.getExpiration().toInstant());
                log.info("REFRESH_TOKEN_ROTATED — OldJTI: {} — UserId: {} — CID: {}",
                        jti, userId, MDC.get("correlationId"));
            }

            return buildAuthResponse(UUID.fromString(userId), role, clientIp, userAgent);

        } catch (ExpiredJwtException ex) {
            log.warn("REFRESH_TOKEN_EXPIRED — Sub: {} — CID: {}",
                    ex.getClaims().getSubject(), MDC.get("correlationId"));
            throw new IllegalArgumentException("Refresh token has expired. Please log in again.");
        } catch (JwtException ex) {
            log.warn("REFRESH_TOKEN_INVALID — {} — CID: {}", ex.getMessage(), MDC.get("correlationId"));
            throw new IllegalArgumentException("Invalid refresh token: " + ex.getMessage());
        }
    }

    // ── Validation ────────────────────────────────────────────────────────

    @Override
    public Optional<AuthPrincipal> validateToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        // Fast cache lookup using token hash to avoid storing full token
        String cacheKey = "validated:" + Integer.toHexString(token.hashCode());
        Cache tokenCache = cacheManager.getCache("token");
        
        if (tokenCache != null) {
            AuthPrincipal cached = tokenCache.get(cacheKey, AuthPrincipal.class);
            if (cached != null) {
                // Fast blacklist check - O(1) lookup
                if (cached.jti() != null && blacklistService.isBlacklisted(cached.jti())) {
                    tokenCache.evict(cacheKey);
                    return Optional.empty();
                }
                return Optional.of(cached);
            }
        }

        // Parse and validate token
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String jti = claims.getId();
            String userIdStr = claims.getSubject();
            String role = claims.get("role", String.class);
            String type = claims.get("type", String.class);

            // Reject refresh tokens used as access tokens
            if ("refresh".equals(type)) {
                return Optional.empty();
            }

            // Blacklist check - O(1)
            if (jti != null && blacklistService.isBlacklisted(jti)) {
                return Optional.empty();
            }

            UUID userId = UUID.fromString(userIdStr);
            AuthPrincipal principal = new AuthPrincipal(userId, role, jti);

            // Cache the validated principal
            if (tokenCache != null) {
                tokenCache.put(cacheKey, principal);
            }

            return Optional.of(principal);

        } catch (ExpiredJwtException ex) {
            return Optional.empty();
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    /**
     * Extracts the JTI for blacklisting. Works on expired tokens too.
     */
    public Optional<String> extractJti(String token) {
        try {
            return Optional.ofNullable(
                Jwts.parser().verifyWith(signingKey).build()
                    .parseSignedClaims(token).getPayload().getId()
            );
        } catch (ExpiredJwtException ex) {
            return Optional.ofNullable(ex.getClaims().getId());
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    /**
     * Decodes all claims for introspection (US 2.2).
     * Works on expired and tampered tokens — shows what went wrong.
     */
    public TokenIntrospection introspect(String token) {
        if (token == null || token.isBlank()) {
            return TokenIntrospection.invalid("Token is null or empty");
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey).build()
                    .parseSignedClaims(token).getPayload();

            String  jti         = claims.getId();
            boolean blacklisted = jti != null && blacklistService.isBlacklisted(jti);

            return new TokenIntrospection(
                    true, blacklisted, null, "HS256",
                    claims.get("type", String.class),
                    jti, claims.getSubject(),
                    claims.get("role", String.class),
                    claims.getIssuedAt()   != null ? claims.getIssuedAt().toInstant()   : null,
                    claims.getExpiration() != null ? claims.getExpiration().toInstant() : null
            );
        } catch (ExpiredJwtException ex) {
            Claims claims = ex.getClaims();
            return new TokenIntrospection(
                    false, false, "Token is EXPIRED", "HS256",
                    claims.get("type", String.class),
                    claims.getId(), claims.getSubject(),
                    claims.get("role", String.class),
                    claims.getIssuedAt()   != null ? claims.getIssuedAt().toInstant()   : null,
                    claims.getExpiration() != null ? claims.getExpiration().toInstant() : null
            );
        } catch (JwtException ex) {
            return TokenIntrospection.invalid("Token is TAMPERED or INVALID: " + ex.getMessage());
        }
    }

    // ── Records ───────────────────────────────────────────────────────────

    public record TokenPair(
            String  accessToken,
            String  refreshToken,
            Instant issuedAt,
            Instant accessExpiresAt,
            Instant refreshExpiresAt
    ) {}

    public record TokenIntrospection(
            boolean valid,
            boolean blacklisted,
            String  error,
            String  algorithm,
            String  tokenType,
            String  jti,
            String  subject,
            String  role,
            Instant issuedAt,
            Instant expiresAt
    ) {
        static TokenIntrospection invalid(String reason) {
            return new TokenIntrospection(false, false, reason, null, null, null, null, null, null, null);
        }
    }
}

