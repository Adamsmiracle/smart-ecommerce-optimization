package com.miracle.smart_ecommerce_security.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * CSRF demonstration controller — User Story 3.1.
 *
 * <h3>Purpose</h3>
 * Illustrates the difference between CSRF-protected and CSRF-exempt endpoints:
 * <ul>
 *   <li>{@code GET /csrf-demo/token} — returns the current CSRF token so a browser
 *       client can include it in subsequent state-changing requests.</li>
 *   <li>{@code POST /csrf-demo/submit} — a simulated form endpoint that requires a valid
 *       CSRF token when called from a browser (Cookie-based CSRF protection is active
 *       for non-{@code /api/**} paths).</li>
 * </ul>
 *
 * <h3>Why CSRF is disabled for {@code /api/**}</h3>
 * REST APIs consumed by JavaScript (fetch/axios) use stateless JWT Bearer tokens.
 * The browser never automatically includes JWT tokens in cross-origin requests
 * (unlike cookies), so CSRF attacks are not possible — the attacker cannot access
 * the victim's JWT. CSRF protection is therefore disabled for all {@code /api/**}
 * and {@code /graphql} endpoints.
 *
 * <h3>When CSRF MUST be enabled</h3>
 * <ul>
 *   <li>Server-side rendered forms (Thymeleaf, JSP) that use session cookies.</li>
 *   <li>Any endpoint where the browser automatically sends credentials (session cookies,
 *       Basic Auth) on every cross-origin request.</li>
 * </ul>
 *
 * <h3>How to test with Postman</h3>
 * <ol>
 *   <li>GET {@code /csrf-demo/token} — note the {@code token} value and the {@code XSRF-TOKEN}
 *       cookie set in the response.</li>
 *   <li>POST {@code /csrf-demo/submit} with header {@code X-XSRF-TOKEN: <token>} — succeeds.</li>
 *   <li>POST {@code /csrf-demo/submit} without the header — returns 403 Forbidden (CSRF blocked).</li>
 * </ol>
 */
@RestController
@RequestMapping("/csrf-demo")
@Tag(name = "CSRF Demo", description = "Demonstrates CSRF protection for form-based endpoints (US 3.1)")
public class CsrfDemoController {

    /**
     * Returns the current CSRF token.
     *
     * <p>Spring Security automatically injects the {@link CsrfToken} into the request
     * when {@code CookieCsrfTokenRepository} is configured. The frontend reads this
     * token and includes it as the {@code X-XSRF-TOKEN} header on all mutating requests.</p>
     */
    @GetMapping("/token")
    @Operation(
        summary = "Get CSRF token",
        description = "Returns the CSRF token required for state-changing requests to /csrf-demo/** endpoints. " +
                      "The token is also set as the XSRF-TOKEN cookie. " +
                      "Note: /api/** endpoints do NOT require CSRF tokens (stateless JWT)."
    )
    public ResponseEntity<Map<String, Object>> getCsrfToken(HttpServletRequest request) {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());

        if (csrfToken == null) {
            return ResponseEntity.ok(Map.of(
                "message", "CSRF token not available — CSRF is disabled for this request path",
                "note", "CSRF is only active for non-/api/** paths"
            ));
        }

        return ResponseEntity.ok(Map.of(
            "token",       csrfToken.getToken(),
            "headerName",  csrfToken.getHeaderName(),
            "paramName",   csrfToken.getParameterName(),
            "usage",       "Include as header: " + csrfToken.getHeaderName() + ": " + csrfToken.getToken(),
            "explanation", "This endpoint is CSRF-protected because it is outside /api/**. " +
                           "POST /csrf-demo/submit will be rejected with 403 if this token is missing."
        ));
    }

    /**
     * Simulated CSRF-protected form submit.
     *
     * <p>Requires the {@code X-XSRF-TOKEN} header (or {@code _csrf} parameter)
     * matching the value from {@code GET /csrf-demo/token}.</p>
     *
     * <p>From Postman: works fine — Postman sends the cookie back automatically.</p>
     * <p>From a browser on a different origin: blocked by CSRF filter → 403 Forbidden.</p>
     */
    @PostMapping(value = "/submit", consumes = {MediaType.APPLICATION_FORM_URLENCODED_VALUE, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
        summary = "CSRF-protected form submit",
        description = "Requires X-XSRF-TOKEN header matching the value from GET /csrf-demo/token. " +
                      "Missing or wrong token → 403 Forbidden. " +
                      "This demonstrates CSRF protection for stateful/form-based endpoints."
    )
    public ResponseEntity<Map<String, String>> submitForm(
            @RequestParam(required = false) String message,
            HttpServletRequest request) {

        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());

        return ResponseEntity.ok(Map.of(
            "status",      "success",
            "message",     "Form submitted successfully — CSRF token was valid",
            "submitted",   message != null ? message : "(no message)",
            "csrfToken",   csrfToken != null ? csrfToken.getToken() : "N/A",
            "explanation", "CSRF protection worked: this request included a valid " +
                           (csrfToken != null ? csrfToken.getHeaderName() : "X-XSRF-TOKEN") + " header"
        ));
    }

    /**
     * Explanation endpoint — no auth, no CSRF — just documentation.
     */
    @GetMapping("/explain")
    @Operation(
        summary = "CSRF vs CORS explanation",
        description = "Returns a human-readable explanation of CSRF and CORS differences"
    )
    public ResponseEntity<Map<String, Object>> explain() {
        return ResponseEntity.ok(Map.of(
            "CSRF", Map.of(
                "fullName",   "Cross-Site Request Forgery",
                "protects",   "Against malicious sites tricking a user's browser into making authenticated requests",
                "mechanism",  "Synchonizer token (hidden form field or X-XSRF-TOKEN header) that an attacker cannot read",
                "enabledFor", "/csrf-demo/** (form endpoints with session cookies)",
                "disabledFor","/api/**, /graphql (stateless JWT — browser never auto-sends JWT as cookie)",
                "tryIt",      "GET /csrf-demo/token → then POST /csrf-demo/submit with and without the token"
            ),
            "CORS", Map.of(
                "fullName",   "Cross-Origin Resource Sharing",
                "protects",   "Against scripts on one origin reading responses from another origin",
                "mechanism",  "Browser sends Origin header; server responds with Access-Control-Allow-Origin",
                "allowedOrigins", new String[]{
                    "http://localhost:3000 (React)",
                    "http://localhost:4200 (Angular)",
                    "http://localhost:5173 (Vite/Vue)"
                },
                "tryIt",      "Send a request from an unlisted origin — browser blocks the response (403 preflight)"
            ),
            "keyDifference", "CSRF = attacking who sends the request. CORS = controlling who reads the response."
        ));
    }
}

