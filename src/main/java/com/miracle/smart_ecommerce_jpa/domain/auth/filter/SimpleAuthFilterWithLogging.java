package com.miracle.smart_ecommerce_jpa.domain.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.miracle.smart_ecommerce_jpa.domain.auth.service.TokenActivityService;
import com.miracle.smart_ecommerce_jpa.domain.auth.service.TokenService;
import com.miracle.smart_ecommerce_jpa.domain.user.repository.UserRepository;
import com.miracle.smart_ecommerce_jpa.domain.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Enhanced authentication filter with comprehensive token activity logging.
 * Tracks token usage, validation patterns, and potential security issues.
 */
@Component
public class SimpleAuthFilterWithLogging extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SimpleAuthFilterWithLogging.class);
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final TokenActivityService tokenActivityService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SimpleAuthFilterWithLogging(UserRepository userRepository, TokenService tokenService, TokenActivityService tokenActivityService) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.tokenActivityService = tokenActivityService;
        log.info("SimpleAuthFilterWithLogging instantiated and ready to process requests");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        String clientIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        log.info("AUTH_REQUEST_START - {} {} - IP: {} - UserAgent: {} - CID: {}", 
            method, requestURI, clientIp, userAgent, MDC.get("correlationId"));

        // Skip auth processing for public endpoints
        if (isPublicEndpoint(requestURI)) {
            log.debug("PUBLIC_ENDPOINT - {} {} - Skipping authentication", method, requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        try {
            boolean authenticated = false;

            String tokenHeader = request.getHeader("X-Auth-Token");
            if (tokenHeader != null && !tokenHeader.isBlank()) {
                Optional<TokenService.AuthPrincipal> principal = tokenService.validateToken(tokenHeader.trim());
                if (principal.isPresent()) {
                    UUID userId = principal.get().userId;
                    String role = principal.get().role;
                    MDC.put("userId", userId.toString());
                    MDC.put("userRole", role);
                    authenticated = true;
                    
                    // Log token activity
                    tokenActivityService.logTokenValidation(tokenHeader, userId.toString(), role, clientIp, userAgent);
                    
                    log.info("AUTH_SUCCESS - {} {} - Method: X-Auth-Token - UserId: {} - Role: {} - IP: {} - CID: {}", 
                        method, requestURI, userId, role, clientIp, MDC.get("correlationId"));
                } else {
                    // Log failed token validation
                    tokenActivityService.logTokenValidationFailure(tokenHeader, "Invalid token", clientIp, userAgent);
                    log.warn("AUTH_FAILURE - {} {} - Method: X-Auth-Token - Invalid token - IP: {} - CID: {}", 
                        method, requestURI, clientIp, MDC.get("correlationId"));
                }
            } else {
                String userIdHeader = request.getHeader("X-User-Id");
                if (userIdHeader != null && !userIdHeader.isBlank()) {
                    try {
                        UUID userId = UUID.fromString(userIdHeader);
                        Optional<User> maybe = userRepository.findById(userId);
                        if (maybe.isPresent()) {
                            User u = maybe.get();
                            MDC.put("userId", u.getId().toString());
                            MDC.put("userRole", u.getRole());
                            authenticated = true;
                            log.info("AUTH_SUCCESS - {} {} - Method: X-User-Id - UserId: {} - Role: {} - IP: {} - CID: {}", 
                                method, requestURI, u.getId(), u.getRole(), clientIp, MDC.get("correlationId"));
                        } else {
                            log.warn("AUTH_FAILURE - {} {} - Method: X-User-Id - Unknown user: {} - IP: {} - CID: {}", 
                                method, requestURI, userIdHeader, clientIp, MDC.get("correlationId"));
                        }
                    } catch (IllegalArgumentException e) {
                        log.warn("AUTH_FAILURE - {} {} - Method: X-User-Id - Invalid UUID: {} - IP: {} - CID: {}", 
                            method, requestURI, userIdHeader, clientIp, MDC.get("correlationId"));
                    }
                } else {
                    log.warn("AUTH_FAILURE - {} {} - No auth headers found - IP: {} - CID: {}", 
                        method, requestURI, clientIp, MDC.get("correlationId"));
                }
            }

            // Reject unauthenticated requests
            if (!authenticated) {
                log.error("ACCESS_DENIED - {} {} - Authentication required - IP: {} - UserAgent: {} - CID: {}", 
                    method, requestURI, clientIp, userAgent, MDC.get("correlationId"));
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.writeValue(response.getOutputStream(), Map.of(
                        "status", 401,
                        "error", "Unauthorized",
                        "message", "Authentication required. Please provide a valid X-Auth-Token header."
                ));
                return;
            }

            filterChain.doFilter(request, response);
        } finally {
            // Always clean up MDC to prevent context leakage
            MDC.remove("userId");
            MDC.remove("userRole");
            log.debug("MDC context cleared for request: {}", requestURI);
        }
    }

    /**
     * Determines if endpoint is public and doesn't require authentication
     */
    private boolean isPublicEndpoint(String requestURI) {
        return requestURI.startsWith("/api/auth") ||
                requestURI.equals("/api/health") ||
                requestURI.startsWith("/swagger-ui") ||
                requestURI.startsWith("/v3/api-docs") ||
                requestURI.equals("/");
    }
    
    /**
     * Extracts client IP address from request
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
