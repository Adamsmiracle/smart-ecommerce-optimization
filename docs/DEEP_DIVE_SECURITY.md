# Deep Dive: Security & Authentication

> **Project:** Smart E-Commerce Security  
> **Stack:** Spring Boot 3.3.5 · Spring Security 6 · Java 21 · JJWT 0.12.6 · Google OAuth2 (OIDC) · PostgreSQL  
> **Date:** March 3, 2026

---

## Table of Contents

1. [The Big Picture](#1-the-big-picture)
2. [The Spring Security Filter Chain](#2-the-spring-security-filter-chain)
3. [Authentication Method 1 — JWT Login](#3-authentication-method-1--jwt-login)
4. [Authentication Method 2 — Google OAuth2](#4-authentication-method-2--google-oauth2)
5. [How Every Request Is Verified](#5-how-every-request-is-verified)
6. [Authorization — Who Can Do What](#6-authorization--who-can-do-what)
7. [Password Security — BCrypt](#7-password-security--bcrypt)
8. [CSRF Protection](#8-csrf-protection)
9. [CORS Policy](#9-cors-policy)
10. [Token Blacklisting — Logout](#10-token-blacklisting--logout)
11. [Security Audit & Monitoring](#11-security-audit--monitoring)
12. [DSA Concepts Used](#12-dsa-concepts-used)
13. [The Complete Request Lifecycle](#13-the-complete-request-lifecycle)

---

## 1. The Big Picture

The application has **two ways** to authenticate and **two layers** to authorize:

```
┌─────────────────────────────────────────────────────────────────┐
│                        CLIENT REQUEST                           │
└────────────────────────────┬────────────────────────────────────┘
                             │
            ┌────────────────▼────────────────┐
            │     AUTHENTICATION (Who are you?)│
            │                                 │
            │  ┌─────────────┐  ┌──────────┐  │
            │  │ JWT Login   │  │ Google   │  │
            │  │ email+pass  │  │ OAuth2   │  │
            │  └──────┬──────┘  └────┬─────┘  │
            │         │              │         │
            │         └──────┬───────┘         │
            │                │                 │
            │         JWT Token issued         │
            └────────────────┬────────────────┘
                             │
            ┌────────────────▼────────────────┐
            │   AUTHORIZATION (What can you do?)│
            │                                  │
            │  Layer 1: URL rules (SecurityConfig)│
            │  Layer 2: @PreAuthorize on methods │
            └──────────────────────────────────┘
```

**Key principle:** Authentication happens once (at login). Every subsequent request proves identity using the JWT — the server never stores sessions for API clients.

---

## 2. The Spring Security Filter Chain

Every HTTP request passes through a chain of filters **before** reaching your controller. Here is the exact chain this app uses (in order):

```
Request arrives
      │
      ▼
1.  DisableEncodeUrlFilter          — Prevents session IDs in URLs
2.  WebAsyncManagerIntegrationFilter — Async security context propagation
3.  SecurityContextHolderFilter     — Sets up the SecurityContext for the request
4.  HeaderWriterFilter              — Adds security headers (X-Frame-Options, etc.)
5.  CorsFilter                      — Handles CORS preflight and headers
6.  CsrfFilter                      — Validates CSRF token (for /csrf-demo form)
7.  LogoutFilter                    — Handles logout requests
8.  OAuth2AuthorizationRequestRedirectFilter — Starts Google OAuth2 flow
9.  OAuth2LoginAuthenticationFilter — Handles Google's callback (/login/oauth2/code/google)
10. JwtAuthenticationFilter ◄─────── OUR CUSTOM FILTER (populates SecurityContext from JWT)
11. RequestCacheAwareFilter         — Restores saved requests after login
12. SecurityContextHolderAwareRequestFilter — Integrates with Servlet API
13. AnonymousAuthenticationFilter   — Sets "anonymousUser" if no auth was set
14. SessionManagementFilter         — Session fixation protection
15. ExceptionTranslationFilter      — Converts security exceptions to 401/403
16. AuthorizationFilter             — Enforces URL-level access rules
      │
      ▼
  Controller / GraphQL Resolver
      │
      ▼
  @PreAuthorize checked (method-level)
```

**Why this order matters:**
- Our `JwtAuthenticationFilter` (step 10) runs BEFORE `AnonymousAuthenticationFilter` (step 13). If a valid JWT is found, the user is authenticated. If not, step 13 marks them as `anonymousUser`.
- `AuthorizationFilter` (step 16) checks URL rules AFTER the JWT filter has had its chance to authenticate.
- If authentication failed and the endpoint requires auth → `ExceptionTranslationFilter` (step 15) converts it to a 401 response.

---

## 3. Authentication Method 1 — JWT Login

### What is JWT?

JWT stands for **JSON Web Token**. It is a self-contained token — all the information about the user is encoded inside the token itself. The server does not need to look up a session in a database to know who you are.

A JWT has three parts separated by dots:
```
eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyLXV1aWQiLCJyb2xlIjoiQURNSU4ifQ.abc123signature
       │                              │                              │
    HEADER                         PAYLOAD                       SIGNATURE
 (algorithm)                     (the claims)              (tamper-proof seal)
```

### The Login Flow — Step by Step

```
Step 1: Client sends credentials
POST /api/auth/login
{
  "email": "admin@smartecommerce.com",
  "password": "password123"
}

Step 2: AuthController receives the request
authenticationManager.authenticate(
    new UsernamePasswordAuthenticationToken(email, password)
)

Step 3: Spring Security's DaoAuthenticationProvider:
  → Calls CustomUserDetailsService.loadUserByUsername(email)
  → Queries the database: SELECT * FROM app_user WHERE email_address = ?
  → Returns the user's stored BCrypt hash
  → BCryptPasswordEncoder.matches("password123", "$2a$10$...hash...")
  → If match → Authentication SUCCESS, fires AuthenticationSuccessEvent
  → If no match → Throws BadCredentialsException → 401

Step 4: JwtTokenService.generateToken(userId, role) builds:
{
  "jti": "random-uuid",          ← unique token ID (for blacklisting)
  "sub": "user-uuid",            ← who this token belongs to
  "role": "ADMIN",               ← their role
  "iat": 1709467200,             ← issued at (Unix timestamp)
  "exp": 1709553600              ← expires at (24 hours later)
}
→ Signs it with HMAC-SHA256 using the secret key from application.yaml

Step 5: Response returned to client
{
  "status": true,
  "data": {
    "userId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "role": "ADMIN",
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  }
}
```

### How the Token Is Signed

The signature is computed as:
```
HMAC-SHA256(
    base64(header) + "." + base64(payload),
    SECRET_KEY
)
```

The secret key is stored in `application.yaml`:
```yaml
jwt:
  secret: c2VjdXJlLWVjb21tZXJjZS1qd3Qtc2VjcmV0...  ← Base64-encoded key
  expiration-ms: 86400000                             ← 24 hours
```

**Why this is secure:** The client can read the header and payload (they are just Base64-encoded, not encrypted), but they **cannot forge the signature** without knowing the secret key. If someone tampers with the payload (e.g., changes `"role": "CUSTOMER"` to `"role": "ADMIN"`), the signature will not match and the server will reject the token.

---

## 4. Authentication Method 2 — Google OAuth2

### What is OAuth2 / OIDC?

**OAuth2** is a protocol for delegated authorization. **OIDC (OpenID Connect)** is built on top of OAuth2 and adds identity — it lets Google prove to your app that a user is who they say they are.

Google uses OIDC. When a user logs in with Google:
- Google authenticates them (username + password + 2FA — Google handles all of this)
- Google gives your app a signed token proving who the user is
- Your app trusts Google's signature

### The Google Login Flow — Step by Step

```
Step 1: User visits
http://localhost:8080/oauth2/authorization/google

Step 2: Spring Security generates a state + nonce, stores them in the session,
and redirects the browser to Google:
https://accounts.google.com/o/oauth2/v2/auth
  ?client_id=942411584483-...
  &redirect_uri=http://localhost:8080/login/oauth2/code/google
  &state=GfWjaTGQ...    ← random value to prevent CSRF on the OAuth2 flow
  &nonce=ei0v4Rnq...    ← random value to prevent replay attacks
  &scope=openid profile email

Step 3: User logs in on Google's page
(Google handles password check, 2FA, etc.)

Step 4: Google redirects back to your app:
http://localhost:8080/login/oauth2/code/google
  ?code=4/0AfrIep...    ← authorization code (one-time use)
  &state=GfWjaTGQ...    ← must match what was stored in session

Step 5: OAuth2LoginAuthenticationFilter intercepts the callback
  → Validates state matches the session value (prevents CSRF)
  → Exchanges the code for tokens by calling Google's token endpoint
  → Validates the ID token's signature (Google's public key)
  → Extracts user info: sub, email, given_name, family_name

Step 6: CustomOAuth2UserService.loadUser() runs
  → Looks up user in DB: findByOauthProviderAndOauthProviderId("google", sub)
  → If NOT found → fallback: findByEmailAddress(email)
  → If STILL not found → creates new User with role="CUSTOMER", saves to DB
  → If found but not linked → sets oauth_provider + oauth_provider_id, saves
  → Embeds the User entity into the OidcUser attributes under key "appUser"

Step 7: OAuth2AuthenticationSuccessHandler runs
  → Reads the User entity from oidcUser.getAttribute("appUser")  ← no DB query
  → Calls JwtTokenService.generateToken(user.getId(), user.getRole())
  → Invalidates the OAuth2 session (JWT replaces it)
  → Writes JSON response:
{
  "status": true,
  "message": "OAuth2 authentication successful",
  "data": {
    "userId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "role": "CUSTOMER",
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "email": "adamsmiracle0@gmail.com"
  }
}
```

### Why the Session Is Needed for OAuth2 (But Not for JWT)

OAuth2 requires a session for **one specific moment**: storing the `state` and `nonce` between Step 2 (redirect to Google) and Step 5 (Google's callback). This takes a few seconds. Without the session, the state cannot be verified and the login fails.

After Step 7, the session is invalidated — the client uses the JWT for everything after that. This is why `SessionCreationPolicy.IF_REQUIRED` is used instead of `STATELESS`.

---

## 5. How Every Request Is Verified

After logging in (via either method), the client sends the JWT with every request:

```
GET /api/users
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyLXV1aWQi...
```

### JwtAuthenticationFilter — Step by Step

```java
// 1. Read the Authorization header
String header = request.getHeader("Authorization");

// 2. Check it starts with "Bearer "
if (header != null && header.startsWith("Bearer ")) {
    String token = header.substring(7);

    // 3. Validate the token
    Optional<AuthPrincipal> principal = tokenService.validateToken(token);

    // 4. If valid, populate SecurityContextHolder
    if (principal.isPresent()) {
        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(
                userId,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))  // or ROLE_CUSTOMER
            );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
// 5. Continue the filter chain regardless
filterChain.doFilter(request, response);
```

### JwtTokenService.validateToken() — What It Checks

```
Token arrives
     │
     ▼
Is it null or blank?           → return empty (invalid)
     │
     ▼
Is the JTI in the blacklist?   → return empty (logged out token)
     │
     ▼
Parse and verify signature:
  Jwts.parser()
      .verifyWith(signingKey)  ← recomputes HMAC-SHA256, compares
      .build()
      .parseSignedClaims(token)
     │
     ├─ ExpiredJwtException    → return empty (expired)
     ├─ JwtException           → return empty (tampered/invalid)
     │
     ▼
Extract claims:
  sub  → userId (UUID)
  role → "ADMIN" or "CUSTOMER"
     │
     ▼
Return AuthPrincipal(userId, role) ← filter uses this to set SecurityContext
```

---

## 6. Authorization — Who Can Do What

Authorization happens at **two layers** after authentication:

### Layer 1 — URL Rules (SecurityConfig)

These rules are checked by `AuthorizationFilter` (step 16 in the chain):

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/**").permitAll()              // Anyone
    .requestMatchers(GET, "/api/products/**").permitAll()    // Anyone (read-only)
    .requestMatchers(GET, "/api/categories/**").permitAll()  // Anyone (read-only)
    .requestMatchers("/swagger-ui/**", ...).permitAll()      // Anyone
    .requestMatchers("/graphql").authenticated()             // Must be logged in
    .requestMatchers("/api/admin/**").hasRole("ADMIN")       // ADMIN only
    .anyRequest().authenticated()                            // Everything else: logged in
)
```

### Layer 2 — Method Rules (@PreAuthorize)

After the URL check passes, Spring's method security intercepts the controller/resolver call:

```java
// On REST controllers:
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> getAllUsers() { ... }

// On GraphQL resolvers:
@PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
public ProductResponse product(@Argument UUID id) { ... }

@PreAuthorize("hasRole('ADMIN')")
public ProductResponse createProduct(@Argument CreateProductRequest input) { ... }
```

**How `hasRole('ADMIN')` works:**
Spring looks at `SecurityContextHolder.getContext().getAuthentication().getAuthorities()` and checks if `ROLE_ADMIN` is in the list. The `JwtAuthenticationFilter` stored `new SimpleGrantedAuthority("ROLE_" + role.toUpperCase())` — so `ROLE_ADMIN` or `ROLE_CUSTOMER` is what Spring looks for.

### Role-to-Endpoint Matrix

| Endpoint / Operation | Public | CUSTOMER | ADMIN |
|---|:---:|:---:|:---:|
| `POST /api/auth/login` | ✅ | ✅ | ✅ |
| `POST /api/auth/register` | ✅ | ✅ | ✅ |
| `GET /api/products/**` | ✅ | ✅ | ✅ |
| `GET /api/categories/**` | ✅ | ✅ | ✅ |
| `POST /api/products` (create) | ❌ | ❌ | ✅ |
| `PUT /api/products/{id}` | ❌ | ❌ | ✅ |
| `DELETE /api/products/{id}` | ❌ | ❌ | ✅ |
| `GET /api/users` (all users) | ❌ | ❌ | ✅ |
| `GET /api/users/{id}` (own) | ❌ | ✅ | ✅ |
| `POST /api/orders` | ❌ | ✅ | ✅ |
| `GET /api/orders` (all) | ❌ | ❌ | ✅ |
| `POST /api/cart/**` | ❌ | ✅ | ✅ |
| `POST /api/reviews` | ❌ | ✅ | ✅ |
| `GET /api/admin/security-report` | ❌ | ❌ | ✅ |
| `POST /graphql` (any query) | ❌ | ✅ | ✅ |
| GraphQL mutations (products/categories) | ❌ | ❌ | ✅ |
| `/csrf-demo` | ✅ | ✅ | ✅ |
| `/oauth2/authorization/google` | ✅ | ✅ | ✅ |

### What Happens When Authorization Fails

```
No token sent → JwtFilter does nothing → AnonymousUser → AuthorizationFilter blocks
→ ExceptionTranslationFilter → authenticationEntryPoint fires:
{
  "status": false,
  "message": "Authentication required. Provide a valid Bearer JWT token.",
  "statusCode": 401
}

Token sent but wrong role (e.g., CUSTOMER hits admin endpoint):
→ JwtFilter authenticates as CUSTOMER → AuthorizationFilter blocks
→ ExceptionTranslationFilter → accessDeniedHandler fires:
{
  "status": false,
  "message": "Access denied. Insufficient role privileges.",
  "statusCode": 403
}
```

---

## 7. Password Security — BCrypt

### What BCrypt Is

BCrypt is a **password hashing function** specifically designed to be slow. This is intentional — it makes brute-force attacks impractical.

A BCrypt hash looks like:
```
$2a$10$dXJ3SW6G7P50lGmMQoeJhOxYfOkNh9V7HHGMuOBJ4OPBF/bBp9MBm
 │   │  └────────────────────────────────────────────────────────┘
 │   │                          hash
 │   └── cost factor (10 = 2^10 = 1,024 iterations)
 └── algorithm version (2a)
```

### Why BCrypt (Not MD5 or SHA-256)

| Property | MD5 / SHA-256 | BCrypt |
|---|---|---|
| Speed | Billions/second | ~100/second at cost=10 |
| Salt | Must add manually | Built-in, automatic |
| Cost tunable | No | Yes — increase cost as hardware gets faster |
| Rainbow table resistant | Partially | Yes — unique salt per password |
| Designed for passwords | No | Yes |

### How It Works in the App

```java
// Storing a password (registration):
String hash = passwordEncoder.encode("password123");
// → "$2a$10$dXJ3SW6G7P50lGmMQoeJh..." (60 characters, different every time)
user.setPasswordHash(hash);
userRepository.save(user);

// Checking a password (login):
// DaoAuthenticationProvider internally calls:
boolean match = passwordEncoder.matches("password123", storedHash);
// BCrypt extracts the salt from the stored hash, re-hashes the input,
// and compares — returns true only if they match
```

### OAuth2 Users and Passwords

OAuth2 users (Google login) have `passwordHash = NULL` in the database. `CustomUserDetailsService` handles this:

```java
.password(user.getPasswordHash() != null ? user.getPasswordHash() : "")
```

An empty string is returned — Spring Security stores it but it is never used for OAuth2 users since they never go through the `DaoAuthenticationProvider` login flow.

---

## 8. CSRF Protection

### What CSRF Is

**Cross-Site Request Forgery** is an attack where a malicious website tricks your browser into making a request to your app using your existing session/cookies.

**Example attack:**
1. You log into your bank at `bank.com` — your browser stores a session cookie.
2. You visit `evil.com` — it has a hidden form that POSTs to `bank.com/transfer`.
3. Your browser automatically includes the `bank.com` cookie with the request.
4. The bank sees a valid session and processes the transfer.

### Why JWT APIs Don't Need CSRF

JWTs are stored in JavaScript (not cookies), and JavaScript cannot be read by other websites due to the **Same-Origin Policy**. `evil.com` cannot read your JWT, so it cannot make authenticated requests to your API. CSRF is therefore **disabled** for all API and GraphQL endpoints:

```java
.csrf(csrf -> csrf
    .ignoringRequestMatchers("/api/**", "/graphql", "/login/oauth2/code/*")
    ...
)
```

### When CSRF IS Needed — The Demo

The `/csrf-demo` endpoint uses a **Thymeleaf HTML form** — a traditional stateful interaction where the browser submits using cookies. CSRF protection IS active here:

```html
<!-- Thymeleaf auto-injects the CSRF token as a hidden field: -->
<form th:action="@{/csrf-demo}" method="post">
    <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}"/>
    ...
</form>
```

When the form is submitted, Spring Security verifies the `_csrf` token. If you submit without the token (or with a wrong one) → **403 Forbidden**.

The CSRF token is also stored in a cookie (`XSRF-TOKEN`) via `CookieCsrfTokenRepository` for SPA compatibility.

---

## 9. CORS Policy

### What CORS Is

**Cross-Origin Resource Sharing** is a browser security mechanism that prevents JavaScript on one website from reading responses from another website.

**Without CORS headers:** If `frontend.com` calls `api.com/data` via `fetch()`, the browser blocks the response (even if the request reaches the server).

**With CORS headers:** `api.com` adds `Access-Control-Allow-Origin: https://frontend.com` to responses, telling the browser it is safe to let `frontend.com` read the data.

### CORS vs CSRF — The Confusion

| | CORS | CSRF |
|---|---|---|
| **What it protects** | Browser from reading cross-origin responses | Server from unauthorized cross-origin state changes |
| **Who enforces it** | The browser | The server |
| **Attack it prevents** | Reading secret data from another site | Making unauthorized requests using your session |
| **JWT APIs need it?** | Yes — to allow your frontend to call the API | No — JWT is not auto-sent by the browser |

### This App's CORS Configuration

```java
CorsConfiguration config = new CorsConfiguration();
config.setAllowedOrigins(List.of(
    "http://localhost:3000",    // React / Vue / Angular dev server
    "http://localhost:4200",    // Angular default
    "http://localhost:8080",    // Same-origin (Swagger UI)
    "http://localhost:63342"    // IntelliJ built-in server
));
config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
config.setAllowedHeaders(List.of("*"));
config.setAllowCredentials(true);    // Allows cookies (needed for CSRF demo)
config.setMaxAge(3600L);             // Cache preflight for 1 hour
```

**Unauthorized origins get no `Access-Control-Allow-Origin` header** — the browser blocks the response automatically.

---

## 10. Token Blacklisting — Logout

### The Problem with JWTs and Logout

JWTs are **stateless** — once issued, the server has no way to invalidate them before they expire. If a user logs out but keeps their token, they could still use it for the remaining 24 hours.

### The Solution — In-Memory Blacklist

When a user logs out, the token's unique ID (`jti`) is stored in a `ConcurrentHashMap`:

```
POST /api/auth/logout
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...

1. JwtTokenService.extractJti(token) → "random-uuid-jti"
2. tokenBlacklistService.blacklist("random-uuid-jti", expiryInstant)
   → blacklistedTokens.put("random-uuid-jti", expiryInstant)
3. Response: 200 "Logged out successfully"

Next request with the same token:
1. JwtAuthenticationFilter calls tokenService.validateToken(token)
2. JwtTokenService.validateToken():
   → extractJti(token) → "random-uuid-jti"
   → blacklistService.isBlacklisted("random-uuid-jti")
   → returns true → return Optional.empty()
3. SecurityContextHolder NOT populated → user treated as anonymous
4. → 401 Unauthorized
```

### Memory Management — Scheduled Purge

The blacklist only needs to store tokens until they expire. Every 15 minutes, a scheduled job cleans up expired entries:

```java
@Scheduled(fixedRate = 900_000)  // Every 15 minutes
public void purgeExpired() {
    Instant now = Instant.now();
    blacklistedTokens.entrySet()
        .removeIf(entry -> entry.getValue().isBefore(now));
}
```

This prevents the HashMap from growing indefinitely if many tokens are blacklisted.

---

## 11. Security Audit & Monitoring

### What Gets Tracked

Spring Security fires application events for every security-significant action. `SecurityEventListener` catches them all:

```
AuthenticationSuccessEvent       → user logged in successfully
AuthenticationFailureBadCredentials → wrong password
AuthorizationDeniedEvent         → tried to access something without permission
```

For each event, the listener:
1. Increments an `AtomicLong` counter (lock-free, thread-safe)
2. Adds a `SecurityEvent` record to a bounded `ConcurrentLinkedDeque` (max 100 entries, newest first)

### The Security Report

`GET /api/admin/security-report` (ADMIN only) returns:

```json
{
  "authenticationStats": {
    "successCount": 42,
    "failureCount": 3,
    "deniedCount": 1
  },
  "tokenStats": {
    "activeTokenCount": 12,
    "totalValidations": 156,
    "blacklistedCount": 4
  },
  "recentSecurityEvents": [
    {
      "type": "AUTH_SUCCESS",
      "principal": "admin@smartecommerce.com",
      "timestamp": "2026-03-03T13:00:00Z"
    },
    {
      "type": "AUTH_FAILURE",
      "principal": "attacker@evil.com",
      "timestamp": "2026-03-03T12:59:55Z"
    }
  ]
}
```

### Detecting Brute Force

If someone is trying to guess a password, the `failureCount` will be high and the `recentSecurityEvents` list will show repeated `AUTH_FAILURE` entries for the same principal — a clear signal of a brute-force attempt.

---

## 12. DSA Concepts Used

| Concept | Where | How |
|---|---|---|
| **HMAC-SHA256 (Hash function)** | JWT signature | `HMAC(key, header.payload)` — O(1) to sign and verify. Tamper-proof without encryption. |
| **BCrypt (Adaptive hash)** | Password storage | Applies 2^10 rounds of hashing with a random salt. Intentionally slow — O(2^cost). |
| **HashMap (ConcurrentHashMap)** | Token blacklist | `jti → expiry` mapping. O(1) average lookup on every request. Thread-safe without locking. |
| **HashMap (ConcurrentHashMap)** | Token activity tracking | `tokenId → TokenActivity`. O(1) update per validation. |
| **Deque (ConcurrentLinkedDeque)** | Recent security events | `addFirst()` for newest-first order. `removeLast()` to enforce 100-entry cap. O(1) both ends. |
| **AtomicLong (CAS counter)** | Auth success/failure counts | Compare-and-swap atomic increment — O(1), no locks, thread-safe under high concurrency. |
| **Scheduled scan** | Blacklist purge | Linear scan O(n) every 15 minutes to remove expired entries. Prevents unbounded growth. |

---

## 13. The Complete Request Lifecycle

### Scenario: ADMIN fetches all users

```
1. ADMIN logged in earlier, has JWT:
   "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyLXV1aWQiLCJyb2xlIjoiQURNSU4ifQ.sig"

2. Sends request:
   GET /api/users
   Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...

3. Filter Chain runs:
   → CsrfFilter: /api/** is ignored — skip
   → OAuth2 filters: no OAuth2 code parameter — skip
   → JwtAuthenticationFilter:
       header = "Bearer eyJhbGciOiJIUzI1NiJ9..."
       token = "eyJhbGciOiJIUzI1NiJ9..."
       validateToken(token):
         → not null ✓
         → jti not in blacklist ✓
         → signature valid (HMAC-SHA256 matches) ✓
         → not expired ✓
         → returns AuthPrincipal(userId="uuid", role="ADMIN")
       Sets SecurityContextHolder:
         Authentication = UsernamePasswordAuthenticationToken
           principal = "user-uuid"
           authorities = [ROLE_ADMIN]
   → AnonymousAuthenticationFilter: context already set — skip
   → AuthorizationFilter:
       checks: .anyRequest().authenticated() — user IS authenticated ✓

4. Request reaches UserController.getAllUsers()
   → @PreAuthorize("hasRole('ADMIN')") checked:
       SecurityContextHolder has ROLE_ADMIN ✓
   → Service layer executes DB query
   → Response: 200 OK with paginated user list

5. Logging:
   → ServiceLoggingAspect logs method entry/exit
   → TokenActivityService logs this validation
```

### Scenario: CUSTOMER tries to fetch all users

```
1. CUSTOMER has JWT with role=CUSTOMER

2. Sends:
   GET /api/users
   Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...[CUSTOMER token]

3. JwtAuthenticationFilter:
   → Token valid ✓
   → Sets SecurityContextHolder: authorities = [ROLE_CUSTOMER]

4. AuthorizationFilter:
   → .anyRequest().authenticated() — passes (they ARE authenticated)

5. UserController.getAllUsers():
   → @PreAuthorize("hasRole('ADMIN')") checked:
       SecurityContextHolder has ROLE_CUSTOMER, NOT ROLE_ADMIN ✗
   → Spring throws AccessDeniedException
   → ExceptionTranslationFilter catches it
   → accessDeniedHandler fires:

Response:
{
  "status": false,
  "message": "Access denied. Insufficient role privileges.",
  "statusCode": 403
}

6. SecurityEventListener catches AuthorizationDeniedEvent:
   → deniedCount.incrementAndGet()
   → Adds to recentEvents deque
   → Logs: SECURITY_EVENT — ACCESS_DENIED — Authentication: user-uuid
```

### Scenario: No token sent to protected endpoint

```
1. Request:
   GET /api/users
   (no Authorization header)

2. JwtAuthenticationFilter:
   → header is null → skip entirely

3. AnonymousAuthenticationFilter:
   → SecurityContextHolder is empty → sets "anonymousUser"

4. AuthorizationFilter:
   → .anyRequest().authenticated() — anonymousUser is NOT authenticated ✗
   → throws AuthenticationException (not AccessDeniedException)

5. ExceptionTranslationFilter:
   → authenticationEntryPoint fires:

Response:
{
  "status": false,
  "message": "Authentication required. Provide a valid Bearer JWT token.",
  "statusCode": 401
}
```

