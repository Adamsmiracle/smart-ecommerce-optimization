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
 * Consolidated authentication filter that handles all authentication with comprehensive logging.
 * Replaces both SimpleAuthFilter and SimpleAuthFilterWithLogging with a single, feature-complete implementation.
 */
@Component
public class ConsolidatedAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ConsolidatedAuthFilter.class);
    
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final TokenActivityService tokenActivityService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ConsolidatedAuthFilter(UserRepository userRepository, TokenService tokenService, TokenActivityService tokenActivityService) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.tokenActivityService = tokenActivityService;
        log.info("ConsolidatedAuthFilter instantiated with comprehensive logging and token activity tracking");
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
            String authMethod = null;
            UUID userId = null;
            String role = null;

            String tokenHeader = request.getHeader("X-Auth-Token");
            if (tokenHeader != null && !tokenHeader.isBlank()) {
                authMethod = "X-Auth-Token";
                Optional<TokenService.AuthPrincipal> principal = tokenService.validateToken(tokenHeader.trim());
                if (principal.isPresent()) {
                    userId = principal.get().userId;
                    role = principal.get().role;
                    MDC.put("userId", userId.toString());
                    MDC.put("userRole", role);
                    authenticated = true;
                    
                    // Log token activity
                    tokenActivityService.logTokenValidation(tokenHeader, userId.toString(), role, clientIp, userAgent);
                    
                    log.info("AUTH_SUCCESS - {} {} - Method: {} - UserId: {} - Role: {} - IP: {} - CID: {}", 
                        method, requestURI, authMethod, userId, role, clientIp, MDC.get("correlationId"));
                } else {
                    // Log failed token validation
                    tokenActivityService.logTokenValidationFailure(tokenHeader, "Invalid token", clientIp, userAgent);
                    log.warn("AUTH_FAILURE - {} {} - Method: {} - Invalid token - IP: {} - CID: {}", 
                        method, requestURI, authMethod, clientIp, MDC.get("correlationId"));
                }
            } else {
                // Handle X-User-Id header (legacy support)
                String userIdHeader = request.getHeader("X-User-Id");
                if (userIdHeader != null && !userIdHeader.isBlank()) {
                    authMethod = "X-User-Id";
                    try {
                        UUID headerUserId = UUID.fromString(userIdHeader);
                        Optional<User> maybe = userRepository.findById(headerUserId);
                        if (maybe.isPresent()) {
                            User u = maybe.get();
                            MDC.put("userId", u.getId().toString());
                            MDC.put("userRole", u.getRole());
                            authenticated = true;
                            log.info("AUTH_SUCCESS - {} {} - Method: {} - UserId: {} - Role: {} - IP: {} - CID: {}", 
                                method, requestURI, authMethod, u.getId(), u.getRole(), clientIp, MDC.get("correlationId"));
                        } else {
                            log.warn("AUTH_FAILURE - {} {} - Method: {} - Unknown user: {} - IP: {} - CID: {}", 
                                method, requestURI, authMethod, userIdHeader, clientIp, MDC.get("correlationId"));
                        }
                    } catch (IllegalArgumentException e) {
                        log.warn("AUTH_FAILURE - {} {} - Method: {} - Invalid UUID: {} - IP: {} - CID: {}", 
                            method, requestURI, authMethod, userIdHeader, clientIp, MDC.get("correlationId"));
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
                        "message", "Authentication required. Please provide a valid X-Auth-Token header.",
                        "timestamp", java.time.Instant.now().toString()
                ));
                return;
            }

            log.debug("AUTH_COMPLETED - {} {} - Authenticated successfully - CID: {}", 
                method, requestURI, MDC.get("correlationId"));

            filterChain.doFilter(request, response);
            
        } catch (Exception e) {
            log.error("AUTH_ERROR - {} {} - Unexpected error: {} - IP: {} - CID: {}", 
                method, requestURI, e.getMessage(), clientIp, MDC.get("correlationId"), e);
            
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), Map.of(
                    "status", 500,
                    "error", "Internal Server Error",
                    "message", "Authentication processing error",
                    "timestamp", java.time.Instant.now().toString()
            ));
        } finally {
            // Always clean up MDC to prevent context leakage
            MDC.remove("userId");
            MDC.remove("userRole");
            log.debug("AUTH_MDC_CLEANED - {} {} - CID: {}", method, requestURI, MDC.get("correlationId"));
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
