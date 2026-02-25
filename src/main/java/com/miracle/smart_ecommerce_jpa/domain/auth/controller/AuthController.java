package com.miracle.smart_ecommerce_jpa.domain.auth.controller;

import com.miracle.smart_ecommerce_jpa.common.response.ApiResponse;
import com.miracle.smart_ecommerce_jpa.domain.auth.dto.AuthRequest;
import com.miracle.smart_ecommerce_jpa.domain.auth.dto.AuthResponse;
import com.miracle.smart_ecommerce_jpa.domain.auth.service.AuthService;
import com.miracle.smart_ecommerce_jpa.domain.auth.service.TokenActivityService; // NEW IMPORT
import com.miracle.smart_ecommerce_jpa.domain.auth.service.TokenService;
import com.miracle.smart_ecommerce_jpa.domain.user.dto.request.CreateUserRequest;
import com.miracle.smart_ecommerce_jpa.domain.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest; // NEW IMPORT
import jakarta.validation.Valid;

// OpenAPI annotations
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;

@Tag(name = "Authentication", description = "Login and registration endpoints")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;
    private final UserService userService;
    private final TokenService tokenService;
    private final TokenActivityService tokenActivityService; // NEW FIELD

    public AuthController(AuthService authService, UserService userService, TokenService tokenService, TokenActivityService tokenActivityService) {
        this.authService = authService;
        this.userService = userService;
        this.tokenService = tokenService;
        this.tokenActivityService = tokenActivityService;
    }

    @Operation(summary = "Authenticate user", description = "Authenticate using email and password. Returns user id and role on success.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Authenticated",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> authenticate(@Valid @RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        log.info("AUTH_LOGIN_REQUEST - Email: {} - IP: {} - UserAgent: {} - CID: {}", 
            request.getEmail(), clientIp, userAgent, MDC.get("correlationId"));
        
        AuthResponse response = authService.authenticate(request.getEmail(), request.getPassword());
        if (response == null) {
            log.warn("AUTH_LOGIN_FAILED - Email: {} - Invalid credentials - IP: {} - UserAgent: {} - CID: {}", 
                request.getEmail(), clientIp, userAgent, MDC.get("correlationId"));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid credentials", 401));
        }
        
        String token = tokenService.generateToken(response.getUserId(), response.getRole());
        response.setToken(token);
        
        // Enhanced token activity logging with request context
        tokenActivityService.logTokenGeneration(token, response.getUserId().toString(), response.getRole(), clientIp, userAgent);
        
        log.info("AUTH_LOGIN_SUCCESS - Email: {} - UserId: {} - Role: {} - IP: {} - UserAgent: {} - CID: {}", 
            request.getEmail(), response.getUserId(), response.getRole(), clientIp, userAgent, MDC.get("correlationId"));
        
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Register user", description = "Register a new user and return the created user's id and role.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User created",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @SecurityRequirements
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody CreateUserRequest request) {
        log.info("AUTH_REGISTER_REQUEST - Email: {} - FirstName: {} - LastName: {} - CID: {}", 
            request.getEmailAddress(), request.getFirstName(), request.getLastName(), MDC.get("correlationId"));
        
        try {
            var created = userService.createUser(request);
            AuthResponse response = AuthResponse.builder().userId(created.getId()).role(created.getRole()).build();
            String token = tokenService.generateToken(response.getUserId(), response.getRole());
            response.setToken(token);
            
            log.info("AUTH_REGISTER_SUCCESS - Email: {} - UserId: {} - Role: {} - CID: {}", 
                request.getEmailAddress(), created.getId(), created.getRole(), MDC.get("correlationId"));
            
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(response, "User registered successfully"));
            
        } catch (Exception e) {
            log.error("AUTH_REGISTER_ERROR - Email: {} - Error: {} - CID: {}", 
                request.getEmailAddress(), e.getMessage(), MDC.get("correlationId"), e);
            throw e; // Re-throw to let existing error handling deal with it
        }
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
