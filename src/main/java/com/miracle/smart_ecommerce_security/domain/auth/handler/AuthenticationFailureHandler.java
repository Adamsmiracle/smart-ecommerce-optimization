package com.miracle.smart_ecommerce_security.domain.auth.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;

/**
 * Custom authentication failure handler that provides detailed error responses
 * for different types of authentication failures.
 */
@Component
@Slf4j
public class AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final ObjectMapper objectMapper;

    public AuthenticationFailureHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, 
                                      HttpServletResponse response,
                                      AuthenticationException exception) throws IOException {
        
        String clientIp = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        String correlationId = MDC.get("correlationId");
        
        log.warn("AUTH_FAILURE — {} — IP: {} — UserAgent: {} — CID: {}", 
                exception.getMessage(), clientIp, userAgent, correlationId);

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        String message = determineErrorMessage(exception);
        
        Map<String, Object> errorResponse = Map.of(
            "status", false,
            "statusCode", 401,
            "message", message,
            "path", request.getRequestURI(),
            "timestamp", Instant.now().toString(),
            "correlationId", correlationId != null ? correlationId : "unknown"
        );

        String jsonResponse = objectMapper.writeValueAsString(errorResponse);
        response.setContentLength(jsonResponse.length());
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }

    private String determineErrorMessage(AuthenticationException exception) {
        if (exception instanceof BadCredentialsException) {
            return "Invalid email or password";
        } else if (exception instanceof DisabledException) {
            return "Account is disabled";
        } else if (exception instanceof LockedException) {
            return "Account is locked";
        } else {
            return "Authentication failed: " + exception.getMessage();
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) return xff.split(",")[0].trim();
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isEmpty()) return xri;
        return request.getRemoteAddr();
    }
}