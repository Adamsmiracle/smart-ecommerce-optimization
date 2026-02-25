package com.miracle.smart_ecommerce_jpa.domain.auth.filter;

import com.fasterxml.jackson.databind.ObjectMapper;                          // NEW IMPORT
import com.miracle.smart_ecommerce_jpa.domain.auth.service.TokenActivityService; // NEW IMPORT
import com.miracle.smart_ecommerce_jpa.domain.auth.service.TokenService;
import com.miracle.smart_ecommerce_jpa.domain.user.repository.UserRepository;
import com.miracle.smart_ecommerce_jpa.domain.user.entity.User;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;                                    // NEW IMPORT
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;                                                         // NEW IMPORT
import java.util.Optional;
import java.util.UUID;

/**
 * Simple filter that reads X-Auth-Token or X-User-Id header and places userId and role into MDC
 * so downstream components can access the authenticated user.
 */
@Component
public class SimpleAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SimpleAuthFilter.class);
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final ObjectMapper objectMapper = new ObjectMapper();             // NEW FIELD

    public SimpleAuthFilter(UserRepository userRepository, TokenService tokenService) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        log.info("SimpleAuthFilter instantiated and ready to process requests");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestURI = request.getRequestURI();

        // Skip auth processing for public endpoints
        if (isPublicEndpoint(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            boolean authenticated = false;                                    // NEW VARIABLE

            String tokenHeader = request.getHeader("X-Auth-Token");
            if (tokenHeader != null && !tokenHeader.isBlank()) {
                Optional<TokenService.AuthPrincipal> principal = tokenService.validateToken(tokenHeader.trim());
                if (principal.isPresent()) {
                    UUID userId = principal.get().userId;
                    String role = principal.get().role;
                    MDC.put("userId", userId.toString());
                    MDC.put("userRole", role);
                    authenticated = true;                                     // NEW LINE
                    log.debug("Authenticated via X-Auth-Token: userId={}, role={}", userId, role);
                } else {
                    log.debug("Invalid X-Auth-Token provided for request: {}", requestURI);
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
                            authenticated = true;                             // NEW LINE
                            log.debug("Authenticated via X-User-Id header: userId={}, role={}", u.getId(), u.getRole());
                        } else {
                            log.warn("X-User-Id header contained unknown userId: {}", userIdHeader);
                        }
                    } catch (IllegalArgumentException e) {
                        log.warn("X-User-Id header contained invalid UUID: {}", userIdHeader);
                    }
                } else {
                    log.debug("No X-Auth-Token or X-User-Id header found in request: {}", requestURI);
                }
            }

            // NEW BLOCK — reject unauthenticated requests
            if (!authenticated) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.writeValue(response.getOutputStream(), Map.of(
                        "status", 401,
                        "error", "Unauthorized",
                        "message", "Authentication required. Please provide a valid X-Auth-Token header."
                ));
                return;
            }
            // END NEW BLOCK

            filterChain.doFilter(request, response);
        } finally {
            // Always clean up MDC to prevent context leakage
            MDC.remove("userId");
            MDC.remove("userRole");
            log.debug("MDC context cleared for request: {}", requestURI);
        }
    }

    /**
     * Determines if the endpoint is public and doesn't require authentication
     */
    private boolean isPublicEndpoint(String requestURI) {
        return requestURI.startsWith("/api/auth") ||
                requestURI.equals("/api/health") ||
                requestURI.startsWith("/swagger-ui") ||          // keep so Swagger UI page loads
                requestURI.startsWith("/v3/api-docs")||
                requestURI.equals("/");

        // keep so Swagger reads the spec
        // REMOVED: /api/products and /api/categories — now protected
    }
}