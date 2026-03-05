package com.miracle.smart_ecommerce_security.domain.auth.controller;

import com.miracle.smart_ecommerce_security.common.response.ApiResponse;
import com.miracle.smart_ecommerce_security.domain.auth.dto.AuthRequest;
import com.miracle.smart_ecommerce_security.domain.auth.dto.AuthResponse;
import com.miracle.smart_ecommerce_security.domain.auth.dto.RefreshRequest;
import com.miracle.smart_ecommerce_security.domain.auth.service.TokenActivityService;
import com.miracle.smart_ecommerce_security.domain.auth.service.TokenBlacklistService;
import com.miracle.smart_ecommerce_security.domain.auth.service.impl.JwtTokenService;
import com.miracle.smart_ecommerce_security.domain.user.dto.request.CreateUserRequest;
import com.miracle.smart_ecommerce_security.domain.user.dto.response.UserResponse;
import com.miracle.smart_ecommerce_security.domain.user.entity.User;
import com.miracle.smart_ecommerce_security.domain.user.repository.UserRepository;
import com.miracle.smart_ecommerce_security.domain.user.service.UserService;
import com.miracle.smart_ecommerce_security.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Optional;

@Tag(name = "Authentication", description = "Login, registration, logout and token refresh endpoints")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService           userService;
    private final UserRepository        userRepository;
    private final TokenActivityService  tokenActivityService;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtTokenService       jwtTokenService;

    // ── Login ─────────────────────────────────────────────────────────────

    @Operation(summary = "Login",
            description = "Authenticate with email + password. Returns a signed access JWT and a refresh token.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Authenticated",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> authenticate(@Valid @RequestBody AuthRequest request,
                                                            HttpServletRequest httpRequest) {
        String clientIp  = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        log.info("AUTH_LOGIN_REQUEST — Email: {} — IP: {} — CID: {}",
                request.getEmail(), clientIp, MDC.get("correlationId"));

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

            User user = userRepository.findByEmailAddress(request.getEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            // AuthResponse (including refreshToken) built entirely in the service
            AuthResponse response = jwtTokenService.buildAuthResponse(
                    user.getId(), user.getRole(), clientIp, userAgent);

            log.info("AUTH_LOGIN_SUCCESS — Email: {} — UserId: {} — Role: {} — IP: {} — CID: {}",
                    request.getEmail(), user.getId(), user.getRole(), clientIp, MDC.get("correlationId"));

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (BadCredentialsException e) {
            log.warn("AUTH_LOGIN_FAILED — Email: {} — IP: {} — CID: {}",
                    request.getEmail(), clientIp, MDC.get("correlationId"));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid credentials", 401));
        }
    }

    // ── Register ──────────────────────────────────────────────────────────

    @Operation(summary = "Register",
            description = "Create a new account. Returns a signed access JWT and a refresh token.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User created",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = AuthResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request")
    })
    @SecurityRequirements
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody CreateUserRequest request,
                                                               HttpServletRequest httpRequest) {
        String clientIp  = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        log.info("AUTH_REGISTER_REQUEST — Email: {} — CID: {}",
                request.getEmailAddress(), MDC.get("correlationId"));

        try {
            UserResponse created = userService.createUser(request);

            // AuthResponse (including refreshToken) built entirely in the service
            AuthResponse response = jwtTokenService.buildAuthResponse(
                    created.getId(), created.getRole(), clientIp, userAgent);

            log.info("AUTH_REGISTER_SUCCESS — Email: {} — UserId: {} — Role: {} — CID: {}",
                    request.getEmailAddress(), created.getId(), created.getRole(), MDC.get("correlationId"));

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.created(response, "User registered successfully"));

        } catch (Exception e) {
            log.error("AUTH_REGISTER_ERROR — Email: {} — Error: {} — CID: {}",
                    request.getEmailAddress(), e.getMessage(), MDC.get("correlationId"), e);
            throw e;
        }
    }

    // ── Refresh ───────────────────────────────────────────────────────────

    @Operation(summary = "Refresh tokens",
            description = "Exchange a valid refresh token for a new access + refresh token pair. " +
                          "The old refresh token is immediately invalidated (token rotation). " +
                          "Call this when the access token expires — do NOT call /login again.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "New token pair issued"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Refresh token expired or invalid")
    })
    @SecurityRequirements
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshRequest request,
                                                              HttpServletRequest httpRequest) {
        String clientIp  = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        log.info("AUTH_REFRESH_REQUEST — IP: {} — CID: {}", clientIp, MDC.get("correlationId"));

        try {
            AuthResponse response = jwtTokenService.refreshAuthResponse(
                    request.getRefreshToken(), clientIp, userAgent);

            log.info("AUTH_REFRESH_SUCCESS — UserId: {} — Role: {} — IP: {} — CID: {}",
                    response.getUserId(), response.getRole(), clientIp, MDC.get("correlationId"));

            return ResponseEntity.ok(ApiResponse.success(response, "Token refreshed successfully"));

        } catch (IllegalArgumentException e) {
            log.warn("AUTH_REFRESH_FAILED — {} — IP: {} — CID: {}",
                    e.getMessage(), clientIp, MDC.get("correlationId"));
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage(), 401));
        }
    }

    // ── Logout ────────────────────────────────────────────────────────────

    @Operation(summary = "Logout",
            description = "Blacklists the current access token so it cannot be reused.")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest httpRequest) {
        String header = httpRequest.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            Optional<String> jti = jwtTokenService.extractJti(token);
            if (jti.isPresent()) {
                tokenBlacklistService.blacklist(jti.get(), Instant.now().plusMillis(86_400_000));
                tokenActivityService.logTokenRevocation(token, "User logout", getClientIp(httpRequest));
                log.info("AUTH_LOGOUT — JTI: {} — IP: {} — CID: {}",
                        jti.get(), getClientIp(httpRequest), MDC.get("correlationId"));
            }
        }
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    // ── Token inspect (US 2.2) ────────────────────────────────────────────

    @Operation(
        summary = "Inspect JWT claims (US 2.2)",
        description = "Decodes a JWT and returns all claims: subject (userId), role, jti, tokenType (access/refresh), " +
                      "issuedAt, expiresAt, algorithm. Works on expired/tampered tokens too. " +
                      "Pass token as ?token=<jwt> or Authorization: Bearer <jwt>."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Token decoded"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "No token provided")
    })
    @SecurityRequirements
    @GetMapping("/token/inspect")
    public ResponseEntity<ApiResponse<JwtTokenService.TokenIntrospection>> inspectToken(
            @RequestParam(required = false) String token,
            HttpServletRequest httpRequest) {

        if (token == null || token.isBlank()) {
            String header = httpRequest.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) token = header.substring(7);
        }
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("No token provided. Pass as ?token=<jwt> or Authorization: Bearer <jwt>", 400));
        }

        JwtTokenService.TokenIntrospection result = jwtTokenService.introspect(token);

        log.info("TOKEN_INSPECT — Valid: {} — Subject: {} — Type: {} — CID: {}",
                result.valid(), result.subject(), result.tokenType(), MDC.get("correlationId"));

        return ResponseEntity.ok(ApiResponse.success(result,
                result.valid() ? "Token is valid" : "Token is invalid: " + result.error()));
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) return xff.split(",")[0].trim();
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isEmpty()) return xri;
        return request.getRemoteAddr();
    }
}

