package com.miracle.smart_ecommerce_jpa.domain.auth.filter;

import com.miracle.smart_ecommerce_jpa.domain.user.repository.UserRepository;
import com.miracle.smart_ecommerce_jpa.domain.user.entity.User;
import org.slf4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

/**
 * Simple filter that reads X-User-Id header (if provided) and places userId and role into MDC
 * so downstream components can access the authenticated user. This is intentionally simple
 * and does not replace a real security framework.
 */
@Component
public class SimpleAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(SimpleAuthFilter.class);
    private final UserRepository userRepository;

    public SimpleAuthFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
        log.info("SimpleAuthFilter instantiated and ready to process requests");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) 
            throws ServletException, IOException {
        String userIdHeader = request.getHeader("X-User-Id");
        String requestURI = request.getRequestURI();
        
        // Skip auth processing for public endpoints
        if (isPublicEndpoint(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }
        
        log.info("SimpleAuthFilter processing request: {} with X-User-Id: {}", requestURI, userIdHeader);
        
        if (userIdHeader != null && !userIdHeader.isBlank()) {
            try {
                UUID userId = UUID.fromString(userIdHeader);
                Optional<User> maybe = userRepository.findById(userId);
                if (maybe.isPresent()) {
                    User u = maybe.get();
                    MDC.put("userId", u.getId().toString());
                    MDC.put("userRole", u.getRole());
                    log.info("User context added to MDC: userId={}, role={}", u.getId(), u.getRole());
                } else {
                    log.warn("X-User-Id header contained unknown userId: {}", userIdHeader);
                }
            } catch (IllegalArgumentException e) {
                log.warn("X-User-Id header contained invalid UUID: {}", userIdHeader);
            }
        } else {
            log.info("No X-User-Id header found in request: {}", requestURI);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Always clean up MDC to prevent context leakage
            MDC.remove("userId");
            MDC.remove("userRole");
            log.info("MDC context cleared for request: {}", requestURI);
        }
    }

    /**
     * Determines if the endpoint is public and doesn't require authentication
     */
    private boolean isPublicEndpoint(String requestURI) {
        return requestURI.startsWith("/api/products") ||
               requestURI.startsWith("/api/categories") ||
               requestURI.startsWith("/api/auth") ||
               requestURI.equals("/api/health") ||
               requestURI.startsWith("/swagger-ui") ||
               requestURI.startsWith("/v3/api-docs");
    }
}
